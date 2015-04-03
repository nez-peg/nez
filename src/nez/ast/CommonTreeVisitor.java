package nez.ast;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

public class CommonTreeVisitor {
	HashMap<Integer, Method> methodMap = new HashMap<Integer, Method>();
	public final Object visit(String method, CommonTree node) {
		Tag tag = node.getTag();
		Method m = invokeMethod(method, tag.id);
		if(m != null) {
			try {
				return m.invoke(this, node);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				System.err.println(node);
				e.printStackTrace();
			}
		}
		return null;
	}
	
	public final Object visit(CommonTree node) {
		return visit("to", node);
	}

	public final boolean isSupported(String method, String tagName) {
		return invokeMethod(method, Tag.tag(tagName).id) != null;
	}
	
	protected Method invokeMethod(String method, int tagId) {
		Integer key = tagId;
		Method m = this.methodMap.get(key);
		if(m == null) {
			String name = method + Tag.tag(tagId).getName();
			try {
				m = this.getClass().getMethod(name, CommonTree.class);
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
				return null;
			} catch (SecurityException e) {
				return null;
			}
			this.methodMap.put(key, m);
		}
		return m;
	}
}
