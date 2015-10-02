package nez.ast.script;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

import nez.ast.Tree;
import nez.ast.TreeVisitor;
import nez.ast.script.asm.ScriptCompiler;

public class Interpreter extends TreeVisitor implements CommonSymbols {
	ScriptContext context;
	TypeSystem base;
	HashMap<String, Object> globalVariables;
	private ScriptCompiler compiler;

	public Interpreter(ScriptContext sc, TypeSystem base) {
		super(TypedTree.class);
		this.context = sc;
		this.base = base;
		this.globalVariables = new HashMap<>();
		this.compiler = new ScriptCompiler(this.base);
	}

	private static EmptyResult empty = new EmptyResult();

	public Object eval(TypedTree node) {
		if (node.size() == 0 && node.getValue() != null) {
			return node.getValue();
		}
		if (node.isStaticNormalMethod) {
			return evalStaticNormalMethod(node);
		}
		return visit("eval", node);
	}

	private Object evalStaticNormalMethod(TypedTree node) {
		TypedTree argsNode = node.get(_param);
		Object[] args = new Object[argsNode.size()];
		for (int i = 0; i < args.length; i++) {
			args[i] = eval(argsNode.get(i));
		}
		return invokeStaticMethod(node.getMethod(), args);
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
			e.getTargetException();
			Throwable cause = e.getTargetException();
			if (cause instanceof RuntimeException) {
				throw (RuntimeException) cause;
			} else {
				throw new RuntimeException(cause);
			}
		}
		return null;
	}

	@Override
	public Object visitUndefinedNode(Tree<?> node) {
		System.out.println("TODO: define " + node);
		return empty;
	}

	public Object evalSource(TypedTree node) {
		Object result = void.class;
		for (TypedTree sub : node) {
			result = eval(sub);
		}
		return result;
	}

	/* TopLevel */

	public Object evalFuncDecl(TypedTree node) {
		this.compiler.compileFuncDecl(node);
		return empty;
	}

	/* Expression Statement */
	public Object evalExpression(TypedTree node) {
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

	public Object evalName(TypedTree node) {
		String name = node.toText();
		if (!this.globalVariables.containsKey(name)) {
			// perror(node, "undefined name: " + name);
		}
		return this.globalVariables.get(name);
	}

	public Object evalAssign(TypedTree node) {
		Object right = eval(node.get(_right));
		String name = node.getText(_left, null);
		this.globalVariables.put(name, right);
		return right;
	}

	public Object evalApply(TypedTree node) {
		String name = node.getText(_name, null);
		Object[] args = (Object[]) eval(node.get(_param));
		return this.invokeStaticMethod(node.getMethod(), args);
	}

	public Object evalList(TypedTree node) {
		Object[] args = new Object[node.size()];
		for (int i = 0; i < node.size(); i++) {
			args[i] = eval(node.get(i));
		}
		return args;
	}

	public Object evalIf(TypedTree node) {
		Boolean cond = (Boolean) eval(node.get(_cond));
		TypedTree thenNode = node.get(_then);
		if (cond) {
			return eval(thenNode);
		} else if (node.size() == 3) {
			TypedTree elseNode = node.get(_else);
			return eval(elseNode);
		}
		return empty;
	}

	public Object evalBlock(TypedTree node) {
		Object retVal = null;
		for (TypedTree child : node) {
			retVal = eval(child);
		}
		return retVal;
	}

	// public Object evalInteger(TypedTree node) {
	// return Integer.parseInt(node.toText());
	// }
	//
	// public Object evalDouble(TypedTree node) {
	// return Double.parseDouble(node.toText());
	// }

	//
	Class<?> typeof(Object o) {
		return o == null ? null : o.getClass();
	}

}