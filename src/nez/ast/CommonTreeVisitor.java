package nez.ast;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

import nez.main.Verbose;
import nez.util.ConsoleUtils;

public class CommonTreeVisitor {
	private HashMap<Integer, Method> methodMap = new HashMap<Integer, Method>();
	public final Object visit(String method, CommonTree node) {
		Tag tag = node.getTag();
		Method m = findMethod(method, tag);
		if(m != null) {
			try {
				return m.invoke(this, node);
			} catch (IllegalAccessException e) {
				Verbose.traceException(e);
			} catch (IllegalArgumentException e) {
				Verbose.traceException(e);
			} catch (InvocationTargetException e) {
				Verbose.traceException(e);
			}
		}
		return toUndefinedNode(node);
	}
	
	public final Object visit(CommonTree node) {
		return visit("to", node);
	}
	
	protected final Method findMethod(String method, Tag tag) {
		Integer key = tag.tagId;
		Method m = this.methodMap.get(key);
		if(m == null) {
			try {
				m = getClassMethod(method, tag);
			} catch (NoSuchMethodException e) {
				Verbose.printNoSuchMethodException(e);
				return null;
			} catch (SecurityException e) {
				Verbose.traceException(e);
				return null;
			}
			this.methodMap.put(key, m);
		}
		return m;
	}
	
	protected Method getClassMethod(String method, Tag tag) throws NoSuchMethodException, SecurityException {
		String name = method + tag.getName();
		return this.getClass().getMethod(name, CommonTree.class);
	}
	
	protected Object toUndefinedNode(CommonTree node) {
		ConsoleUtils.exit(1, "undefined node:" + node);
		return null;
	}
}
