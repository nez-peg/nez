package nez.ast.script.asm;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

import javax.lang.model.type.NullType;

import nez.ast.jcode.JCodeOperator;
import nez.ast.script.CommonSymbols;
import nez.ast.script.TypeSystem;
import nez.ast.script.TypedTree;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class ScriptCompilerAsm implements CommonSymbols {
	// private Map<String, Class<?>> generatedClassMap = new HashMap<String,
	// Class<?>>();
	private TypeSystem typeSystem;
	private ScriptClassLoader cLoader;
	private ClassBuilder cBuilder;
	private MethodBuilder mBuilder;

	// private Stack<MethodBuilder> mBuilderStack = new Stack<MethodBuilder>();

	public ScriptCompilerAsm(TypeSystem typeSystem, ScriptClassLoader cLoader) {
		this.typeSystem = typeSystem;
		this.cLoader = cLoader;

	}

	public void openClass(String name) {
		this.cBuilder = new ClassBuilder("nez/ast/script/" + name, null, null, null);
	}

	public Class<?> closeClass() {
		cLoader.setDump(true);
		Class<?> c = cLoader.definedAndLoadClass(this.cBuilder.getQualifiedClassName(), cBuilder.toByteArray());
		this.cBuilder = null; //
		return c;
	}

	HashMap<String, Method> methodMap = new HashMap<String, Method>();
	private VarEntry var;

	public final void visit(TypedTree node) {
		Method m = lookupMethod("visit", node.getTag().getSymbol());
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
				m = this.getClass().getMethod(name, TypedTree.class);
			} catch (NoSuchMethodException e) {
				return null;
			} catch (SecurityException e) {
				return null;
			}
			this.methodMap.put(tagName, m);
		}
		return m;
	}

	public Class<?> compileFuncDecl(String className, TypedTree node) {
		this.openClass(className);
		this.visitFuncDecl(node);
		return this.closeClass();
	}

	private Class<?> typeof(TypedTree node) {
		// node.getTypedClass();
		return typeSystem.typeof(node);
	}

	public void visitFuncDecl(TypedTree node) {
		// this.mBuilderStack.push(this.mBuilder);
		TypedTree nameNode = node.get(_name);
		TypedTree args = node.get(_param);
		String name = nameNode.toText();
		Class<?> funcType = typeof(nameNode);
		Class<?>[] paramTypes = new Class<?>[args.size()];
		for (int i = 0; i < paramTypes.length; i++) {
			paramTypes[i] = typeof(args.get(i));
		}
		this.mBuilder = this.cBuilder.newMethodBuilder(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, funcType, name, paramTypes);
		this.mBuilder.enterScope();
		for (TypedTree arg : args) {
			this.mBuilder.defineArgument(arg.getText(_name, null), typeof(arg));
		}
		visit(node.get(_body));
		this.mBuilder.exitScope();
		// this.mBuilder.returnValue();
		this.mBuilder.endMethod();
	}

	// FIXME Block scope
	public void visitBlock(TypedTree node) {
		this.mBuilder.enterScope();
		for (TypedTree stmt : node) {
			visit(stmt);
		}
		this.mBuilder.exitScope();
	}

	public void visitVarDecl(TypedTree node) {
		if (node.size() > 1) {
			TypedTree varNode = node.get(_name);
			TypedTree valueNode = node.get(_expr);
			visit(valueNode);
			varNode.setType(typeof(valueNode));
			this.mBuilder.createNewVarAndStore(varNode.toText(), typeof(valueNode));
		}
	}

	public void visitApply(TypedTree node) {
		String name = node.getText(_name, null);
		TypedTree argsNode = node.get(_param);
		Class<?>[] args = new Class<?>[argsNode.size()];
		for (int i = 0; i < args.length; i++) {
			args[i] = typeof(argsNode.get(i));
		}
		Method function = typeSystem.findCompiledMethod(name, args);
		this.mBuilder.callStaticMethod(function.getDeclaringClass(), function.getReturnType(), function.getName(), args);
	}

	public void visitIf(TypedTree node) {
		visit(node.get(_cond));
		this.mBuilder.push(true);

		Label elseLabel = this.mBuilder.newLabel();
		Label mergeLabel = this.mBuilder.newLabel();

		this.mBuilder.ifCmp(Type.BOOLEAN_TYPE, this.mBuilder.NE, elseLabel);

		// then
		visit(node.get(_then));
		this.mBuilder.goTo(mergeLabel);

		// else
		this.mBuilder.mark(elseLabel);
		visit(node.get(_else));

		// merge
		this.mBuilder.mark(mergeLabel);
	}

	public void visitWhile(TypedTree node) {
		Label beginLabel = this.mBuilder.newLabel();
		Label condLabel = this.mBuilder.newLabel();

		this.mBuilder.goTo(condLabel);

		// Block
		this.mBuilder.mark(beginLabel);
		visit(node.get(_body));

		// Condition
		this.mBuilder.mark(condLabel);
		visit(node.get(_cond));
		this.mBuilder.push(true);

		this.mBuilder.ifCmp(Type.BOOLEAN_TYPE, this.mBuilder.EQ, beginLabel);
	}

	public void visitDoWhile(TypedTree node) {
		Label beginLabel = this.mBuilder.newLabel();

		// Do
		this.mBuilder.mark(beginLabel);
		visit(node.get(_body));

		// Condition
		visit(node.get(_cond));
		this.mBuilder.push(true);

		this.mBuilder.ifCmp(Type.BOOLEAN_TYPE, this.mBuilder.EQ, beginLabel);
	}

	public void visitFor(TypedTree node) {
		Label beginLabel = this.mBuilder.newLabel();
		Label condLabel = this.mBuilder.newLabel();

		// Initialize
		visit(node.get(_init));

		this.mBuilder.goTo(condLabel);

		// Block
		this.mBuilder.mark(beginLabel);
		visit(node.get(_body));
		visit(node.get(_iter));

		// Condition
		this.mBuilder.mark(condLabel);
		visit(node.get(_cond));
		this.mBuilder.push(true);
		this.mBuilder.ifCmp(Type.BOOLEAN_TYPE, this.mBuilder.EQ, beginLabel);
	}

	public void visitAssign(TypedTree node) {
		String name = node.getText(_name, null);
		TypedTree valueNode = node.get(_expr);
		VarEntry var = this.mBuilder.getVar(name);
		visit(valueNode);
		if (var != null) {
			this.mBuilder.storeToVar(var);
		} else {
			this.mBuilder.createNewVarAndStore(name, typeof(valueNode));
		}
	}

	public void visitExpression(TypedTree node) {
		this.visit(node.get(0));
	}

	public void visitReturn(TypedTree node) {
		this.visit(node.get(_expr));
		this.mBuilder.returnValue();
	}

	public void visitName(TypedTree node) {
		VarEntry var = this.mBuilder.getVar(node.toText());
		// node.setType(var.getVarClass());
		this.mBuilder.loadFromVar(var);
	}

	private Class<?> typeInfferBinary(TypedTree binary, TypedTree left, TypedTree right) {
		Class<?> leftType = typeof(left);
		Class<?> rightType = typeof(right);
		if (leftType == int.class) {
			if (rightType == int.class) {
				if (binary.getTag().getSymbol().equals("Div")) {
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
		throw new RuntimeException("type error: " + left + ", " + right);
	}

	public void visitBinaryNode(TypedTree node) {
		TypedTree left = node.get(_left);
		TypedTree right = node.get(_right);
		this.visit(left);
		this.visit(right);
		node.setType(typeInfferBinary(node, left, right));
		this.mBuilder.callStaticMethod(JCodeOperator.class, typeof(node), node.getTag().getSymbol(), typeof(left), typeof(right));
	}

	public void visitCompNode(TypedTree node) {
		TypedTree left = node.get(_left);
		TypedTree right = node.get(_right);
		this.visit(left);
		this.visit(right);
		node.setType(boolean.class);
		this.mBuilder.callStaticMethod(JCodeOperator.class, typeof(node), node.getTag().getSymbol(), typeof(left), typeof(right));
	}

	public void visitAdd(TypedTree node) {
		this.visitBinaryNode(node);
	}

	public void visitSub(TypedTree node) {
		this.visitBinaryNode(node);
	}

	public void visitMul(TypedTree node) {
		this.visitBinaryNode(node);
	}

	public void visitDiv(TypedTree node) {
		this.visitBinaryNode(node);
	}

	public void visitNotEquals(TypedTree node) {
		this.visitCompNode(node);
	}

	public void visitLessThan(TypedTree node) {
		this.visitCompNode(node);
	}

	public void visitLessThanEquals(TypedTree node) {
		this.visitCompNode(node);
	}

	public void visitGreaterThan(TypedTree node) {
		this.visitCompNode(node);
	}

	public void visitGreaterThanEquals(TypedTree node) {
		this.visitCompNode(node);
	}

	public void visitLogicalAnd(TypedTree node) {
		this.visitCompNode(node);
	}

	public void visitLogicalOr(TypedTree node) {
		this.visitCompNode(node);
	}

	//
	// public void visitContinue(TypedTree node) {
	// }
	//
	// public void visitBreak(TypedTree node) {
	// }
	//
	// public void visitReturn(TypedTree node) {
	// this.mBuilder.returnValue();
	// }
	//
	// public void visitThrow(TypedTree node) {
	// }
	//
	// public void visitWith(TypedTree node) {
	// }
	//
	// public void visitExpression(TypedTree node) {
	// this.visit(node.get(0));
	// }
	//

	//
	// public void visitApply(TypedTree node) {
	// // TypedTree fieldNode = node.get(_recv"));
	// TypedTree argsNode = node.get(_param);
	// TypedTree name = node.get(_name);
	// // VarEntry var = null;
	//
	// Class<?>[] argTypes = new Class<?>[argsNode.size()];
	// for (int i = 0; i < argsNode.size(); i++) {
	// TypedTree arg = argsNode.get(i);
	// this.visit(arg);
	// argTypes[i] = arg.getTypedClass();
	// }
	// org.objectweb.asm.commons.Method method =
	// Methods.method(node.getTypedClass(), name.toText(), argTypes);
	// this.mBuilder.invokeStatic(this.cBuilder.getTypeDesc(), method);
	// // var = this.scope.getLocalVar(top.toText());
	// // if (var != null) {
	// // this.mBuilder.loadFromVar(var);
	// //
	// // } else {
	// // this.generateRunTimeLibrary(top, argsNode);
	// // this.popUnusedValue(node);
	// // return;
	// // }
	// }
	//
	// public void generateRunTimeLibrary(TypedTree fieldNode, TypedTree
	// argsNode) {
	// String classPath = "";
	// String methodName = null;
	// for (int i = 0; i < fieldNode.size(); i++) {
	// if (i < fieldNode.size() - 2) {
	// classPath += fieldNode.get(i).toText();
	// classPath += ".";
	// } else if (i == fieldNode.size() - 2) {
	// classPath += fieldNode.get(i).toText();
	// } else {
	// methodName = fieldNode.get(i).toText();
	// }
	// }
	// Type[] argTypes = new Type[argsNode.size()];
	// for (int i = 0; i < argsNode.size(); i++) {
	// TypedTree arg = argsNode.get(i);
	// this.visit(arg);
	// argTypes[i] = Type.getType(arg.getTypedClass());
	// }
	// this.mBuilder.callDynamicMethod("nez/ast/jcode/StandardLibrary",
	// "bootstrap", methodName, classPath, argTypes);
	// }
	//
	// public void visitField(TypedTree node) {
	// TypedTree top = node.get(0);
	// VarEntry var = null;
	// if (_Name.equals(top.getTag())) {
	// var = this.scope.getLocalVar(top.toText());
	// if (var != null) {
	// this.mBuilder.loadFromVar(var);
	// } else {
	// // TODO
	// return;
	// }
	// } else {
	// visit(top);
	// }
	// for (int i = 1; i < node.size(); i++) {
	// TypedTree member = node.get(i);
	// if (_Name.equals(member.getTag())) {
	// this.mBuilder.getField(Type.getType(var.getVarClass()), member.toText(),
	// Type.getType(Object.class));
	// visit(member);
	// }
	// }
	// }
	//
	// //
	// public void visitUnaryNode(TypedTree node) {
	// TypedTree child = node.get(0);
	// this.visit(child);
	// node.setType(this.typeInfferUnary(node.get(0)));
	// this.mBuilder.callStaticMethod(JCodeOperator.class, node.getTypedClass(),
	// node.getTag().getSymbol(), child.getTypedClass());
	// this.popUnusedValue(node);
	// }
	//
	// public void visitPlus(TypedTree node) {
	// this.visitUnaryNode(node);
	// }
	//
	// public void visitMinus(TypedTree node) {
	// this.visitUnaryNode(node);
	// }
	//
	// private void evalPrefixInc(TypedTree node, int amount) {
	// TypedTree nameNode = node.get(0);
	// VarEntry var = this.scope.getLocalVar(nameNode.toText());
	// if (var != null) {
	// node.setType(int.class);
	// this.mBuilder.callIinc(var, amount);
	// if (!node.requiredPop) {
	// this.mBuilder.loadFromVar(var);
	// }
	// } else {
	// throw new RuntimeException("undefined variable " + nameNode.toText());
	// }
	// }
	//
	// private void evalSuffixInc(TypedTree node, int amount) {
	// TypedTree nameNode = node.get(0);
	// VarEntry var = this.scope.getLocalVar(nameNode.toText());
	// if (var != null) {
	// node.setType(int.class);
	// if (!node.requiredPop) {
	// this.mBuilder.loadFromVar(var);
	// }
	// this.mBuilder.callIinc(var, amount);
	// } else {
	// throw new RuntimeException("undefined variable " + nameNode.toText());
	// }
	// }
	//
	// public void visitSuffixInc(TypedTree node) {
	// this.evalSuffixInc(node, 1);
	// }
	//
	// public void visitSuffixDec(TypedTree node) {
	// this.evalSuffixInc(node, -1);
	// }
	//
	// public void visitPrefixInc(TypedTree node) {
	// this.evalPrefixInc(node, 1);
	// }
	//
	// public void visitPrefixDec(TypedTree node) {
	// this.evalPrefixInc(node, -1);
	// }
	//
	// private Class<?> typeInfferUnary(TypedTree node) {
	// Class<?> nodeType = node.getTypedClass();
	// if (nodeType == int.class) {
	// return int.class;
	// } else if (nodeType == double.class) {
	// return double.class;
	// }
	// throw new RuntimeException("type error: " + node);
	// }

	public void visitNull(TypedTree p) {
		p.setType(NullType.class);
		this.mBuilder.pushNull();
	}

	// void visitArray(TypedTree p){
	// this.mBuilder.newArray(Object.class);
	// }

	public void visitList(TypedTree node) {
		for (TypedTree element : node) {
			visit(element);
		}
	}

	public void visitTrue(TypedTree p) {
		p.setType(boolean.class);
		this.mBuilder.push(true);
	}

	public void visitFalse(TypedTree p) {
		p.setType(boolean.class);
		this.mBuilder.push(false);
	}

	public void visitInt(TypedTree p) {
		p.setType(int.class);
		this.mBuilder.push(Integer.parseInt(p.toText()));
	}

	public void visitInteger(TypedTree p) {
		this.visitInt(p);
	}

	public void visitOctalInteger(TypedTree p) {
		p.setType(int.class);
		this.mBuilder.push(Integer.parseInt(p.toText(), 8));
	}

	public void visitHexInteger(TypedTree p) {
		p.setType(int.class);
		this.mBuilder.push(Integer.parseInt(p.toText(), 16));
	}

	public void visitDouble(TypedTree p) {
		p.setType(double.class);
		this.mBuilder.push(Double.parseDouble(p.toText()));
	}

	public void visitString(TypedTree p) {
		p.setType(String.class);
		this.mBuilder.push(p.toText());
	}

	public void visitCharacter(TypedTree p) {
		p.setType(String.class);
		this.mBuilder.push(p.toText());
		// p.setType(char.class);
		// this.mBuilder.push(p.toText().charAt(0));
	}

	public void visitUndefined(TypedTree p) {
		System.out.println("undefined: " + p.getTag().getSymbol());
	}

}
