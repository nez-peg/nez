package nez.ast.script;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import konoha.Array;
import konoha.IArray;
import nez.ast.Tree;
import nez.ast.TreeVisitor;
import nez.ast.script.asm.ScriptCompiler;

public class Interpreter extends TreeVisitor implements CommonSymbols {
	ScriptContext context;
	TypeSystem typeSystem;
	private ScriptCompiler compiler;

	public Interpreter(ScriptContext sc, TypeSystem base) {
		super(TypedTree.class);
		this.context = sc;
		this.typeSystem = base;
		this.compiler = new ScriptCompiler(this.typeSystem);
	}

	private static EmptyResult empty = new EmptyResult();

	public Object eval(TypedTree node) {
		switch (node.hint) {
		case Apply:
			return evalApply(node);
		case Constant:
			return node.getValue();
		case MethodApply:
			return evalMethodApply(node);
		case StaticInvocation:
			return evalStaticInvocation(node);
		case GetField:
			return evalField(node);
		case SetField:
			return evalSetField(node);
		case Unique:
		default:
			return visit("eval", node);
		}
	}

	public Object nullEval(TypedTree node) {
		return (node == null) ? null : eval(node);
	}

	@Override
	public Object visitUndefinedNode(Tree<?> node) {
		System.out.println("TODO: define " + node);
		return empty;
	}

	private Object invokeStaticMethod(Method m, Object... args) {
		return this.invokeMethod(m, null, args);
	}

	private Object invokeMethod(Method m, Object self, Object... args) {
		try {
			return m.invoke(self, args);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			System.err.println(m.toString() + ": " + e.getMessage());
			// e.printStackTrace();
		} catch (InvocationTargetException e) {
			Throwable w = e.getTargetException();
			if (w instanceof RuntimeException) {
				throw (RuntimeException) e.getTargetException();
			}
			throw new ScriptRuntimeException(w.getMessage());
		}
		throw new ScriptRuntimeException("failed invocation: " + m);
	}

	public Object evalSource(TypedTree node) {
		Object result = empty;
		for (TypedTree sub : node) {
			if (sub.is(_Error)) {
				return empty;
			}
			result = eval(sub);
		}
		return result;
	}

	/* TopLevel */

	public Object evalFuncDecl(TypedTree node) {
		this.compiler.compileFuncDecl(node);
		return empty;
	}

	/* boolean */

	public Object evalBlock(TypedTree node) {
		Object retVal = null;
		for (TypedTree child : node) {
			retVal = eval(child);
		}
		return retVal;
	}

	public Object evalIf(TypedTree node) {
		boolean cond = (Boolean) eval(node.get(_cond));
		if (cond) {
			return eval(node.get(_then));
		} else {
			TypedTree elseNode = node.get(_else, null);
			if (elseNode != null) {
				return eval(elseNode);
			}
		}
		return empty;
	}

	/* Expression Statement */
	public Object evalExpression(TypedTree node) {
		if (node.getType() == void.class) {
			eval(node.get(0));
			return empty;
		}
		return eval(node.get(0));
	}

	public Object evalEmpty(TypedTree node) {
		return empty;
	}

	/* Expression */

	public Object evalCast(TypedTree node) {
		Object v = eval(node.get(_expr));
		Method m = node.getMethod();
		if (m != null) {
			return this.invokeMethod(m, null, v);
		}
		return v;
	}

	// public Object evalName(TypedTree node) {
	// String name = node.toText();
	// if (!this.globalVariables.containsKey(name)) {
	// // perror(node, "undefined name: " + name);
	// }
	// return this.globalVariables.get(name);
	// }
	//
	// public Object evalAssign(TypedTree node) {
	// Object right = eval(node.get(_right));
	// String name = node.getText(_left, null);
	// return right;
	// }

	public Object evalField(TypedTree node) {
		Field f = node.getField();
		Object recv = null;
		if (!Modifier.isStatic(f.getModifiers())) {
			recv = eval(node.get(_recv));
		}
		// System.out.println("eval field:" + recv + " . " + f);
		try {
			Object v = f.get(recv);
			return v;
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new ScriptRuntimeException(e.getMessage());
		}
	}

	private Object evalSetField(TypedTree node) {
		Object recv = null;
		Object value = eval(node.get(_expr));
		Field f = node.getField();
		if (!Modifier.isStatic(f.getModifiers())) {
			recv = nullEval(node.get(_recv, null));
		}
		try {
			f.set(recv, value);
			return value;
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new ScriptRuntimeException(e.getMessage());
		}
	}

	private Object evalStaticInvocation(TypedTree node) {
		Object[] args = this.evalApplyArgument(node);
		return invokeStaticMethod(node.getMethod(), args);
	}

	public Object evalMethodApply(TypedTree node) {
		Object recv = eval(node.get(_recv));
		Object[] args = evalApplyArgument(node.get(_param));
		return this.invokeMethod(node.getMethod(), recv, args);
	}

	public Object evalApply(TypedTree node) {
		// String name = node.getText(_name, null);
		Object[] args = evalApplyArgument(node.get(_param));
		return this.invokeStaticMethod(node.getMethod(), args);
	}

	private Object[] evalApplyArgument(TypedTree node) {
		Object[] args = new Object[node.size()];
		for (int i = 0; i < node.size(); i++) {
			args[i] = eval(node.get(i));
		}
		return args;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Object evalArray(TypedTree node) {
		Object[] args = evalApplyArgument(node);
		Class<?> atype = node.getClassType();
		if (atype == IArray.class) {
			Object a = new IArray<Object>(args, args.length);
			System.out.println("AAA " + a + " " + a.getClass());
		}
		return new Array(args, args.length);
	}

}