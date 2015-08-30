package nez.ast.jcode;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import javax.lang.model.type.NullType;

import nez.ast.Tag;
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
	private VarEntry var;

	public final void visit(JCodeTree node) {
		Method m = lookupMethod("visit", node.getTag().getName());
		if (m != null) {
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
		if (m == null) {
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

	protected final void popUnusedValue(JCodeTree node) {
		if (node.requiredPop) {
			this.mBuilder.pop();
		}
	}

	public void visitSource(JCodeTree node) {
		node.requirePop();
		this.mBuilder = this.cBuilder.newMethodBuilder(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, void.class, "main");
		this.mBuilder.enterScope();
		for (JCodeTree child : node) {
			this.visit(child);
		}
		this.mBuilder.exitScope();
		this.mBuilder.returnValue(); // return stack top value
		this.mBuilder.endMethod();
		this.cBuilder.visitEnd();
	}

	public void visitBlock(JCodeTree node) {
		node.requirePop();
		for (JCodeTree stmt : node) {
			visit(stmt);
		}
	}

	public void visitFuncDecl(JCodeTree node) {
		this.mBuilderStack.push(this.mBuilder);
		JCodeTree nameNode = node.get(0);
		JCodeTree args = node.get(1);
		String name = nameNode.toText();
		Class<?> funcType = nameNode.getTypedClass();
		Class<?>[] paramClasses = new Class<?>[args.size()];
		for (int i = 0; i < paramClasses.length; i++) {
			paramClasses[i] = args.get(i).getTypedClass();
		}
		// System.out.println(funcType.toGenericString());
		// System.out.println(paramClasses[0].toGenericString());
		this.mBuilder = this.cBuilder.newMethodBuilder(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, funcType, name, paramClasses);
		this.mBuilder.enterScope();
		this.pushScope();
		for (JCodeTree arg : args) {
			this.scope.setLocalVar(arg.toText(), this.mBuilder.defineArgument(arg.getTypedClass()));
		}
		visit(node.get(2));
		this.mBuilder.exitScope();
		this.popScope();
		this.mBuilder.returnValue();
		this.mBuilder.endMethod();
		// this.mBuilder = this.mBuilderStack.pop();
	}

	public void visitVarDeclStmt(JCodeTree node) {
		visit(node.get(0));
	}

	public void visitVarDecl(JCodeTree node) {
		if (node.size() > 1) {
			JCodeTree varNode = node.get(0);
			JCodeTree valueNode = node.get(1);
			visit(valueNode);
			varNode.setType(valueNode.getTypedClass());
			this.scope.setLocalVar(varNode.toText(), this.mBuilder.createNewVarAndStore(valueNode.getTypedClass()));
		}
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
		Label condLabel = this.mBuilder.newLabel();

		this.mBuilder.goTo(condLabel);

		// Block
		this.mBuilder.mark(beginLabel);
		visit(node.get(1));

		// Condition
		this.mBuilder.mark(condLabel);
		visit(node.get(0));
		this.mBuilder.push(true);

		this.mBuilder.ifCmp(Type.BOOLEAN_TYPE, this.mBuilder.EQ, beginLabel);
	}

	public void visitDoWhile(JCodeTree node) {
		Label beginLabel = this.mBuilder.newLabel();

		// Do
		this.mBuilder.mark(beginLabel);
		visit(node.get(0));

		// Condition
		visit(node.get(1));
		this.mBuilder.push(true);

		this.mBuilder.ifCmp(Type.BOOLEAN_TYPE, this.mBuilder.EQ, beginLabel);
	}

	public void visitFor(JCodeTree node) {
		Label beginLabel = this.mBuilder.newLabel();
		Label condLabel = this.mBuilder.newLabel();
		node.requirePop();

		// Initialize
		visit(node.get(0));

		this.mBuilder.goTo(condLabel);

		// Block
		this.mBuilder.mark(beginLabel);
		visit(node.get(3));
		visit(node.get(2));

		// Condition
		this.mBuilder.mark(condLabel);
		visit(node.get(1));
		this.mBuilder.push(true);
		this.mBuilder.ifCmp(Type.BOOLEAN_TYPE, this.mBuilder.EQ, beginLabel);
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

	public void visitAssign(JCodeTree node) {
		JCodeTree nameNode = node.get(0);
		JCodeTree valueNode = node.get(1);
		VarEntry var = this.scope.getLocalVar(nameNode.toText());
		visit(valueNode);
		if (var != null) {
			this.mBuilder.storeToVar(var);
		} else {
			this.scope.setLocalVar(nameNode.toText(), this.mBuilder.createNewVarAndStore(valueNode.getTypedClass()));
		}
	}

	public void visitApply(JCodeTree node) {
		JCodeTree fieldNode = node.get(0);
		JCodeTree argsNode = node.get(1);
		JCodeTree top = fieldNode.get(0);
		VarEntry var = null;
		if (Tag.tag("Name").equals(top.getTag())) {
			var = this.scope.getLocalVar(top.toText());
			if (var != null) {
				this.mBuilder.loadFromVar(var);
			} else {
				this.generateRunTimeLibrary(fieldNode, argsNode);
				this.popUnusedValue(node);
				return;
			}
		}
	}

	public void generateRunTimeLibrary(JCodeTree fieldNode, JCodeTree argsNode) {
		String classPath = "";
		String methodName = null;
		for (int i = 0; i < fieldNode.size(); i++) {
			if (i < fieldNode.size() - 2) {
				classPath += fieldNode.get(i).toText();
				classPath += ".";
			} else if (i == fieldNode.size() - 2) {
				classPath += fieldNode.get(i).toText();
			} else {
				methodName = fieldNode.get(i).toText();
			}
		}
		Type[] argTypes = new Type[argsNode.size()];
		for (int i = 0; i < argsNode.size(); i++) {
			JCodeTree arg = argsNode.get(i);
			this.visit(arg);
			argTypes[i] = Type.getType(arg.getTypedClass());
		}
		this.mBuilder.callDynamicMethod("nez/ast/jcode/StandardLibrary", "bootstrap", methodName, classPath, argTypes);
	}

	public void visitField(JCodeTree node) {
		JCodeTree top = node.get(0);
		VarEntry var = null;
		if (Tag.tag("Name").equals(top.getTag())) {
			var = this.scope.getLocalVar(top.toText());
			if (var != null) {
				this.mBuilder.loadFromVar(var);
			} else {
				// TODO
				return;
			}
		} else {
			visit(top);
		}
		for (int i = 1; i < node.size(); i++) {
			JCodeTree member = node.get(i);
			if (Tag.tag("Name").equals(member.getTag())) {
				this.mBuilder.getField(Type.getType(var.getVarClass()), member.toText(), Type.getType(Object.class));
				visit(member);
			}
		}
	}

	public void visitBinaryNode(JCodeTree node) {
		JCodeTree left = node.get(0);
		JCodeTree right = node.get(1);
		this.visit(left);
		this.visit(right);
		node.setType(typeInfferBinary(node, left, right));
		this.mBuilder.callStaticMethod(JCodeOperator.class, node.getTypedClass(), node.getTag().getName(), left.getTypedClass(), right.getTypedClass());
		this.popUnusedValue(node);
	}

	public void visitCompNode(JCodeTree node) {
		JCodeTree left = node.get(0);
		JCodeTree right = node.get(1);
		this.visit(left);
		this.visit(right);
		node.setType(boolean.class);
		this.mBuilder.callStaticMethod(JCodeOperator.class, node.getTypedClass(), node.getTag().getName(), left.getTypedClass(), right.getTypedClass());
		this.popUnusedValue(node);
	}

	private Class<?> typeInfferBinary(JCodeTree binary, JCodeTree left, JCodeTree right) {
		Class<?> leftType = left.getTypedClass();
		Class<?> rightType = right.getTypedClass();
		if (leftType == int.class) {
			if (rightType == int.class) {
				if (binary.getTag().getName().equals("Div")) {
					return double.class;
				}
				return int.class;
			} else if (rightType == double.class) {
				return double.class;
			} else if (rightType == String.class) {
				return String.class;
			}
		} else if (leftType == double.class) {
			if (rightType == int.class) {
				return double.class;
			} else if (rightType == double.class) {
				return double.class;
			} else if (rightType == String.class) {
				return String.class;
			}
		} else if (leftType == String.class) {
			return String.class;
		} else if (leftType == boolean.class) {
			if (rightType == boolean.class) {
				return boolean.class;
			} else if (rightType == String.class) {
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

	public void visitNotEquals(JCodeTree node) {
		this.visitCompNode(node);
	}

	public void visitLessThan(JCodeTree node) {
		this.visitCompNode(node);
	}

	public void visitLessThanEquals(JCodeTree node) {
		this.visitCompNode(node);
	}

	public void visitGreaterThan(JCodeTree node) {
		this.visitCompNode(node);
	}

	public void visitGreaterThanEquals(JCodeTree node) {
		this.visitCompNode(node);
	}

	public void visitLogicalAnd(JCodeTree node) {
		this.visitCompNode(node);
	}

	public void visitLogicalOr(JCodeTree node) {
		this.visitCompNode(node);
	}

	public void visitUnaryNode(JCodeTree node) {
		JCodeTree child = node.get(0);
		this.visit(child);
		node.setType(this.typeInfferUnary(node.get(0)));
		this.mBuilder.callStaticMethod(JCodeOperator.class, node.getTypedClass(), node.getTag().getName(), child.getTypedClass());
		this.popUnusedValue(node);
	}

	public void visitPlus(JCodeTree node) {
		this.visitUnaryNode(node);
	}

	public void visitMinus(JCodeTree node) {
		this.visitUnaryNode(node);
	}

	private void evalPrefixInc(JCodeTree node, int amount) {
		JCodeTree nameNode = node.get(0);
		VarEntry var = this.scope.getLocalVar(nameNode.toText());
		if (var != null) {
			node.setType(int.class);
			this.mBuilder.callIinc(var, amount);
			if (!node.requiredPop) {
				this.mBuilder.loadFromVar(var);
			}
		} else {
			new RuntimeException("undefined variable " + nameNode.toText());
		}
	}

	private void evalSuffixInc(JCodeTree node, int amount) {
		JCodeTree nameNode = node.get(0);
		VarEntry var = this.scope.getLocalVar(nameNode.toText());
		if (var != null) {
			node.setType(int.class);
			if (!node.requiredPop) {
				this.mBuilder.loadFromVar(var);
			}
			this.mBuilder.callIinc(var, amount);
		} else {
			new RuntimeException("undefined variable " + nameNode.toText());
		}
	}

	public void visitSuffixInc(JCodeTree node) {
		this.evalSuffixInc(node, 1);
	}

	public void visitSuffixDec(JCodeTree node) {
		this.evalSuffixInc(node, -1);
	}

	public void visitPrefixInc(JCodeTree node) {
		this.evalPrefixInc(node, 1);
	}

	public void visitPrefixDec(JCodeTree node) {
		this.evalPrefixInc(node, -1);
	}

	private Class<?> typeInfferUnary(JCodeTree node) {
		Class<?> nodeType = node.getTypedClass();
		if (nodeType == int.class) {
			return int.class;
		} else if (nodeType == double.class) {
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

	public void visitName(JCodeTree node) {
		VarEntry var = this.scope.getLocalVar(node.toText());
		node.setType(var.getVarClass());
		this.mBuilder.loadFromVar(var);
	}

	public void visitList(JCodeTree node) {
		for (JCodeTree element : node) {
			visit(element);
		}
	}

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
		this.mBuilder.push(Integer.parseInt(p.toText()));
	}

	public void visitOctalInteger(JCodeTree p) {
		p.setType(int.class);
		this.mBuilder.push(Integer.parseInt(p.toText(), 8));
	}

	public void visitHexInteger(JCodeTree p) {
		p.setType(int.class);
		this.mBuilder.push(Integer.parseInt(p.toText(), 16));
	}

	public void visitDouble(JCodeTree p) {
		p.setType(double.class);
		this.mBuilder.push(Double.parseDouble(p.toText()));
	}

	public void visitString(JCodeTree p) {
		p.setType(String.class);
		this.mBuilder.push(p.toText());
	}

	public void visitCharacter(JCodeTree p) {
		p.setType(String.class);
		this.mBuilder.push(p.toText());
		// p.setType(char.class);
		// this.mBuilder.push(p.toText().charAt(0));
	}

	public void visitUndefined(JCodeTree p) {
		System.out.println("undefined: " + p.getTag().getName());
	}

}
