package nez.ast.script;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

import nez.ast.Tree;
import nez.ast.TreeVisitor;
import nez.ast.script.asm.ScriptCompiler;
import nez.util.ConsoleUtils;

public class Interpreter extends TreeVisitor implements CommonSymbols {
	ScriptContext context;
	TypeSystem base;
	HashMap<String, Object> globalVariables;

	public Interpreter(ScriptContext sc, TypeSystem base) {
		this.context = sc;
		this.base = base;
		this.globalVariables = new HashMap<>();
	}

	private static EmptyResult empty = new EmptyResult();

	public Object eval(Tree<?> unode) {
		if (unode instanceof TypedTree) {
			TypedTree node = (TypedTree) unode;
			if (node.size() == 0 && node.getValue() != null) {
				return node.getValue();
			}
			Method m = node.getMethod();
			// System.out.println("untyped: method " + m);
			if (m != null) {
				if (node.size() == 2 && node.indexOf(_right) != -1 && node.indexOf(_left) != -1) {
					return invokeMethod(m, null, eval(node.get(_left)), eval(node.get(_right)));
				}
			}
		}
		// System.out.println("untyped");
		return visit("eval", unode);
	}

	private Object invokeMethod(Method m, Object self, Object... args) {
		try {
			return m.invoke(self, args);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.getTargetException();
			throw (RuntimeException) e.getTargetException();
		}
		return null;
	}

	@Override
	public Object visitUndefinedNode(Tree<?> node) {
		System.out.println("TODO: define " + node);
		return empty;
	}

	public Object evalSource(Tree<?> node) {
		Object result = void.class;
		for (Tree<?> sub : node) {
			result = eval(sub);
		}
		return result;
	}

	/* TopLevel */

	public Object evalImport(Tree<?> node) {
		String path = (String) eval(node.get(0));
		try {
			base.addBaseClass(path);
		} catch (ClassNotFoundException e) {
			perror(node, "undefined class name: %s", path);
		}
		return empty;
	}

	public Object evalQualifiedName(Tree<?> node) {
		StringBuilder sb = new StringBuilder();
		s(sb, node);
		return sb.toString();
	}

	public void s(StringBuilder sb, Tree<?> node) {
		Tree<?> prefix = node.get(_prefix);
		if (prefix.size() == 2) {
			s(sb, prefix);
		} else {
			sb.append(prefix.toText());
		}
		sb.append(".").append(node.getText(_name, null));
	}

	public Object evalExpression(Tree<?> node) {
		return eval(node.get(0));
	}

	public Object evalCast(Tree<?> node) {
		Object v = eval(node.get(_expr));
		Method m = ((TypedTree) node).getMethod();
		if (m != null) {
			return this.invokeMethod(m, null, v);
		}
		return v;
	}

	public Object evalAdd(Tree<?> node) {
		Object left = eval(node.get(_left));
		Object right = eval(node.get(_right));
		return evalOperator(node, "opAdd", left, right);
	}

	public Object evalSub(Tree<?> node) {
		Object left = eval(node.get(_left));
		Object right = eval(node.get(_right));
		return evalOperator(node, "opSub", left, right);
	}

	public Object evalMul(Tree<?> node) {
		Object left = eval(node.get(_left));
		Object right = eval(node.get(_right));
		return evalOperator(node, "opMul", left, right);
	}

	public Object evalDiv(Tree<?> node) {
		Object left = eval(node.get(_left));
		Object right = eval(node.get(_right));
		return evalOperator(node, "opDiv", left, right);
	}

	public Object evalEquals(Tree<?> node) {
		Object left = eval(node.get(_left));
		Object right = eval(node.get(_right));
		return evalOperator(node, "opEquals", left, right);
	}

	public Object evalNotEquals(Tree<?> node) {
		Object left = eval(node.get(_left));
		Object right = eval(node.get(_right));
		return evalOperator(node, "opNotEquals", left, right);
	}

	public Object evalLessThan(Tree<?> node) {
		Object left = eval(node.get(_left));
		Object right = eval(node.get(_right));
		return evalOperator(node, "opLessThan", left, right);
	}

	public Object evalGreaterThan(Tree<?> node) {
		Object left = eval(node.get(_left));
		Object right = eval(node.get(_right));
		return evalOperator(node, "opGreaterThan", left, right);
	}

	public Object evalLessThanEquals(Tree<?> node) {
		Object left = eval(node.get(_left));
		Object right = eval(node.get(_right));
		return evalOperator(node, "opLessThanEquals", left, right);
	}

	public Object evalGreaterThanEquals(Tree<?> node) {
		Object left = eval(node.get(_left));
		Object right = eval(node.get(_right));
		return evalOperator(node, "opGreaterThanEquals", left, right);
	}

	public Object evalName(Tree<?> node) {
		String name = node.toText();
		if (!this.globalVariables.containsKey(name)) {
			perror(node, "undefined name: " + name);
		}
		return this.globalVariables.get(name);
	}

	public Object evalAssign(Tree<?> node) {
		Object right = eval(node.get(_right));
		String name = node.getText(_left, null);
		this.globalVariables.put(name, right);
		return right;
	}

	public Object evalFuncDecl(Tree<?> node) {
		ScriptCompiler compiler = new ScriptCompiler(this.base);
		compiler.compileFuncDecl(node);
		return null;
	}

	public Object evalApply(Tree<?> node) {
		String name = node.getText(_name, null);
		Object[] args = (Object[]) eval(node.get(_param));
		return evalFunction(node, name, args);
	}

	public Object evalList(Tree<?> node) {
		Object[] args = new Object[node.size()];
		for (int i = 0; i < node.size(); i++) {
			args[i] = eval(node.get(i));
		}
		return args;
	}

	public Object evalIf(Tree<?> node) {
		Boolean cond = (Boolean) eval(node.get(_cond));
		Tree<?> thenNode = node.get(_then);
		if (cond) {
			return eval(thenNode);
		} else if (node.size() == 3) {
			Tree<?> elseNode = node.get(_else);
			return eval(elseNode);
		}
		return empty;
	}

	public Object evalBlock(Tree<?> node) {
		Object retVal = null;
		for (Tree<?> child : node) {
			retVal = eval(child);
		}
		return retVal;
	}

	public Object evalInteger(Tree<?> node) {
		return Integer.parseInt(node.toText());
	}

	public Object evalDouble(Tree<?> node) {
		return Double.parseDouble(node.toText());
	}

	private void perror(Tree<?> node, String msg) {
		msg = node.formatSourceMessage("error", msg);
		ConsoleUtils.println(msg);
		throw new RuntimeException(msg);
	}

	private void perror(Tree<?> node, String fmt, Object... args) {
		perror(node, String.format(fmt, args));
	}

	//
	Class<?> typeof(Object o) {
		return o == null ? null : o.getClass();
	}

	Object evalOperator(Tree<?> node, String name, Object a1, Object a2) {
		Method m = base.findCompiledMethod(name, typeof(a1), typeof(a2));
		if (m == null) {
			perror(node, "undefined operator: %s %s", typeof(a1), typeof(a2));
		}
		return this.invokeMethod(m, null, a1, a2);
	}

	Object evalFunction(Tree<?> node, String name, Object... args) {
		Class<?>[] classArray = new Class<?>[args.length];
		for (int i = 0; i < args.length; i++) {
			classArray[i] = typeof(args[i]);
		}
		Method m = base.findCompiledMethod(name, classArray);
		if (m == null) {
			perror(node, "undefined function: %s", name);
		}
		return this.invokeMethod(m, null, args);
	}

}