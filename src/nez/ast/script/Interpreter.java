package nez.ast.script;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import nez.ast.Tree;
import nez.ast.TreeVisitor;

public class Interpreter extends TreeVisitor implements KonohaSymbols {
	ClassRepository base;

	public Interpreter(ClassRepository base) {
		this.base = base;
	}

	public Object eval(Tree<?> node) {
		return visit("eval", node);
	}

	public Object evalAdd(Tree<?> node) {
		Object left = eval(node.get(0));
		Object right = eval(node.get(1));
		return evalOperator(node, "opAdd", left, right);
	}

	public Object evalSub(Tree<?> node) {
		Object left = eval(node.get(0));
		Object right = eval(node.get(1));
		return evalOperator(node, "opSub", left, right);
	}

	public Object evalMul(Tree<?> node) {
		Object left = eval(node.get(0));
		Object right = eval(node.get(1));
		return evalOperator(node, "opMul", left, right);
	}

	public Object evalDiv(Tree<?> node) {
		Object left = eval(node.get(0));
		Object right = eval(node.get(1));
		return evalOperator(node, "opDiv", left, right);
	}

	public Object evalInteger(Tree<?> node) {
		return Integer.parseInt(node.toText());
	}

	//
	Class<?> typeof(Object o) {
		return o == null ? null : o.getClass();
	}

	Object evalOperator(Tree<?> node, String name, Object a1, Object a2) {
		Method m = base.findMethod(name, typeof(a1), typeof(a2));
		System.out.println("name: " + m);
		if (m == null) {
			System.out.println(node.formatSourceMessage("error", "undefined operator: " + typeof(a1) + typeof(a2)));
		} else {
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
		}
		throw new RuntimeException("fail: " + name);
	}
}