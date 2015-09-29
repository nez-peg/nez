package nez.ast.script;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

import nez.ast.Tree;
import nez.ast.TreeVisitor;
import nez.util.ConsoleUtils;

public class Interpreter extends TreeVisitor implements KonohaSymbols {
	TypeSystem base;
	HashMap<String, Object> globalVariables;

	public Interpreter(TypeSystem base) {
		this.base = base;
		this.globalVariables = new HashMap<>();
	}

	private static EmptyResult empty = new EmptyResult();

	public Object eval(Tree<?> node) {
		try {
			return visit("eval", node);
		} catch (RuntimeException e) {
		}
		return empty;
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

	public Object evalExpression(Tree<?> node) {
		return eval(node.get(0));
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

	public Object evalImport(Tree<?> node) {
		String path = (String) eval(node.get(0));
		try {
			base.add(path);
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
		Method m = base.findMethod(name, typeof(a1), typeof(a2));
		if (m == null) {
			perror(node, "undefined operator: %s %s", typeof(a1), typeof(a2));
		}
		try {
			return m.invoke(null, a1, a2);
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return empty;
	}

	Object evalFunction(Tree<?> node, String name, Object... args) {
		Class<?>[] classArray = new Class<?>[args.length];
		for (int i = 0; i < args.length; i++) {
			classArray[i] = typeof(args[i]);
		}
		Method m = base.findMethod(name, classArray);
		if (m == null) {
			perror(node, "undefined function: %s", name);
		}
		try {
			return m.invoke(null, args);
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return empty;
	}

}