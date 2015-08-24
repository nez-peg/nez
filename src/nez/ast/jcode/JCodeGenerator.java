package nez.ast.jcode;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import javax.lang.model.type.NullType;

import nez.ast.jcode.ClassBuilder.MethodBuilder;
import nez.ast.jcode.ClassBuilder.VarEntry;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class JCodeGenerator {
	private Map<String, Class<?>> generatedClassMap = new HashMap<String, Class<?>>();
	private final static String packagePrefix = "nez/ast/jcode/";

	private static int nameSuffix = -1;

	private ClassBuilder cBuilder;
	private UserDefinedClassLoader cLoader;
	private MethodBuilder mBuilder;
	private Stack<MethodBuilder> mBuilderStack = new Stack<MethodBuilder>();

	class JCodeScope {
		JCodeScope prev;
		Map<String, nez.ast.jcode.ClassBuilder.VarEntry> varMap;

		public JCodeScope() {
			this.varMap = new HashMap<String, VarEntry>();
		}

		public JCodeScope(JCodeScope prev) {
			this.prev = prev;
			this.varMap = new HashMap<String, VarEntry>();
		}

		public void setLocalVar(String name, VarEntry var) {
			this.varMap.put(name, var);
		}

		public VarEntry getLocalVar(String name) {
			VarEntry var = this.varMap.get(name);
			if(var == null) {
				System.out.println("local variable '" + name + "' is not found");
				System.exit(1);
			}
			return var;
		}
	}

	JCodeScope scope = new JCodeScope();

	public void pushScope() {
		this.scope = new JCodeScope(this.scope);
	}

	public JCodeScope popScope() {
		JCodeScope ret = this.scope;
		this.scope = this.scope.prev;
		return ret;
	}

	public JCodeGenerator(String name) {
		this.cBuilder = new ClassBuilder(packagePrefix + name + ++nameSuffix, null, null, null);
		this.cLoader = new UserDefinedClassLoader();
	}

	public Class<?> generateClass() {
		UserDefinedClassLoader loader = new UserDefinedClassLoader();
		loader.setDump(true);
		return loader.definedAndLoadClass(this.cBuilder.getInternalName(), cBuilder.toByteArray());
	}

	HashMap<String, Method> methodMap = new HashMap<String, Method>();

	public final void visit(JCodeTree node) {
		Method m = lookupMethod("visit", node.getTag().getName());
		if(m != null) {
			try {
				m.invoke(this, node);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		} else {
			visitUndefined(node);
		}
	}

	protected final Method lookupMethod(String method, String tagName) {
		Method m = this.methodMap.get(tagName);
		if(m == null) {
			String name = method + tagName;
			try {
				m = this.getClass().getMethod(name, JCodeTree.class);
			} catch (NoSuchMethodException e) {
				return null;
			} catch (SecurityException e) {
				return null;
			}
			this.methodMap.put(tagName, m);
		}
		return m;
	}

	public void visitSource(JCodeTree node) {
		this.mBuilder = this.cBuilder.newMethodBuilder(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, void.class, "main");
		this.mBuilder.enterScope();
		for(JCodeTree child : node) {
			this.visit(child);
		}
		this.mBuilder.exitScope();
		this.mBuilder.returnValue(); // return stack top value
		this.mBuilder.endMethod();
		this.cBuilder.visitEnd();
	}
	
	public void visitIf(JCodeTree node) {
		visit(node.get(0));
		this.mBuilder.push(true);

		Label elseLabel = this.mBuilder.newLabel();
		Label mergeLabel = this.mBuilder.newLabel();

		this.mBuilder.ifCmp(Type.BOOLEAN_TYPE, this.mBuilder.NE, elseLabel);

		// then
		visit(node.get(1));
		this.mBuilder.goTo(mergeLabel);

		// else
		this.mBuilder.mark(elseLabel);
		visit(node.get(2));

		// merge
		this.mBuilder.mark(mergeLabel);
	}

	public void visitWhile(JCodeTree node) {
		Label beginLabel = this.mBuilder.newLabel();
		Label endLabel = this.mBuilder.newLabel();

		//Condition
		this.mBuilder.mark(beginLabel);
		visit(node.get(0));
		this.mBuilder.push(true);

		this.mBuilder.ifCmp(Type.BOOLEAN_TYPE, this.mBuilder.NE, endLabel);

		//Block
		visit(node.get(1));
		this.mBuilder.goTo(beginLabel);

		this.mBuilder.mark(endLabel);
	}

	public void visitDoWhile(JCodeTree node) {
		Label beginLabel = this.mBuilder.newLabel();

		//Do
		this.mBuilder.mark(beginLabel);
		visit(node.get(0));

		//Condition
		visit(node.get(1));
		this.mBuilder.push(false);

		this.mBuilder.ifCmp(Type.BOOLEAN_TYPE, this.mBuilder.NE, beginLabel);
	}

	public void visitFor(JCodeTree node) {
		Label beginLabel = this.mBuilder.newLabel();
		Label endLabel = this.mBuilder.newLabel();

		//Initialize
		visit(node.get(0));

		//Condition
		this.mBuilder.mark(beginLabel);
		visit(node.get(1));
		this.mBuilder.push(false);
		this.mBuilder.ifCmp(Type.BOOLEAN_TYPE, this.mBuilder.NE, endLabel);

		//Block
		visit(node.get(3));
		visit(node.get(2));
		this.mBuilder.goTo(beginLabel);
		this.mBuilder.mark(endLabel);
	}

	public void visitContinue(JCodeTree node) {
	}

	public void visitBreak(JCodeTree node) {
	}

	public void visitReturn(JCodeTree node) {
		this.mBuilder.returnValue();
	}

	public void visitThrow(JCodeTree node) {
	}

	public void visitWith(JCodeTree node) {
	}

	public void visitApply(JCodeTree node) {
		String classPath = "";
		String methodName = null;
		JCodeTree fieldNode = node.get(0);
		JCodeTree argsNode = node.get(1);
		for(int i = 0; i < fieldNode.size(); i++) {
			if(i < fieldNode.size() - 2) {
				classPath += fieldNode.get(i).toText();
				classPath += ".";
			} else if(i == fieldNode.size() - 2) {
				classPath += fieldNode.get(i).toText();
			} else {
				methodName = fieldNode.get(i).toText();
			}
		}
		Type[] argTypes = new Type[argsNode.size()];
		for(int i = 0; i < argsNode.size(); i++) {
			JCodeTree arg = argsNode.get(i);
			this.visit(arg);
			argTypes[i] = Type.getType(arg.getTypedClass());
		}
		this.mBuilder.callDynamicMethod("nez/ast/jcode/StandardLibrary", "bootstrap", methodName, classPath, argTypes);
	}

	public void visitBinaryNode(JCodeTree node) {
		JCodeTree left = node.get(0);
		JCodeTree right = node.get(1);
		this.visit(left);
		this.visit(right);
		node.setType(typeInfferBinary(node, left, right));
		this.mBuilder.callStaticMethod(JCodeOperator.class, node.getTypedClass(), node.getTag().getName(),
				left.getTypedClass(), right.getTypedClass());
	}

	private Class<?> typeInfferBinary(JCodeTree binary, JCodeTree left, JCodeTree right) {
		Class<?> leftType = left.getTypedClass();
		Class<?> rightType = right.getTypedClass();
		if(leftType == int.class) {
			if(rightType == int.class) {
				if(binary.getTag().getName().equals("Div")) {
					return double.class;
				}
				return int.class;
			} else if(rightType == double.class) {
				return double.class;
			} else if(rightType == String.class) {
				return String.class;
			}
		} else if(leftType == double.class) {
			if(rightType == int.class) {
				return double.class;
			} else if(rightType == double.class) {
				return double.class;
			} else if(rightType == String.class) {
				return String.class;
			}
		} else if(leftType == String.class) {
			return String.class;
		} else if(leftType == boolean.class) {
			if(rightType == boolean.class) {
				return boolean.class;
			} else if(rightType == String.class) {
				return String.class;
			}
		}
		new RuntimeException("type error: " + left + ", " + right);
		return null;
	}

	public void visitAdd(JCodeTree node) {
		this.visitBinaryNode(node);
	}

	public void visitSub(JCodeTree node) {
		this.visitBinaryNode(node);
	}

	public void visitMul(JCodeTree node) {
		this.visitBinaryNode(node);
	}

	public void visitDiv(JCodeTree node) {
		this.visitBinaryNode(node);
	}

	public void visitUnaryNode(JCodeTree node) {
		JCodeTree child = node.get(0);
		this.visit(child);
		node.setType(this.typeInfferUnary(node.get(0)));
		this.mBuilder.callStaticMethod(JCodeOperator.class, node.getTypedClass(), node.getTag().getName(),
				child.getTypedClass());
	}

	public void visitPlus(JCodeTree node) {
		this.visitUnaryNode(node);
	}

	public void visitMinus(JCodeTree node) {
		this.visitUnaryNode(node);
	}

	private Class<?> typeInfferUnary(JCodeTree node) {
		Class<?> nodeType = node.getTypedClass();
		if(nodeType == int.class) {
			return int.class;
		} else if(nodeType == double.class) {
			return double.class;
		}
		new RuntimeException("type error: " + node);
		return null;
	}

	public void visitNull(JCodeTree p) {
		p.setType(NullType.class);
		this.mBuilder.pushNull();
	}

	// void visitArray(JCodeTree p){
	// this.mBuilder.newArray(Object.class);
	// }

	public void visitTrue(JCodeTree p) {
		p.setType(boolean.class);
		this.mBuilder.push(true);
	}

	public void visitFalse(JCodeTree p) {
		p.setType(boolean.class);
		this.mBuilder.push(false);
	}

	public void visitInteger(JCodeTree p) {
		p.setType(int.class);
		this.mBuilder.push((Integer)Integer.parseInt(p.toText()));
	}

	public void visitOctalInteger(JCodeTree p) {
		p.setType(int.class);
		this.mBuilder.push((Integer)Integer.parseInt(p.toText(), 8));
	}

	public void visitHexInteger(JCodeTree p){
		p.setType(int.class);
		this.mBuilder.push((Integer)Integer.parseInt(p.toText(), 16));
	}

	public void visitDouble(JCodeTree p) {
		p.setType(double.class);
		this.mBuilder.push((Double)Double.parseDouble(p.toText()));
	}

	public void visitString(JCodeTree p) {
		p.setType(String.class);
		this.mBuilder.push(p.toText());
	}

	public void visitCharacter(JCodeTree p) {
		p.setType(String.class);
		this.mBuilder.push(p.toText());
		//p.setType(char.class);
		//this.mBuilder.push(p.getText().charAt(0));
	}

	public void visitUndefined(JCodeTree p) {
		System.out.println("undefined: " + p.getTag().getName());
	}

}
