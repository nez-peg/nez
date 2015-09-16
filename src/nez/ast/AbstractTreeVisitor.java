package nez.ast;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

import nez.main.Verbose;
import nez.util.ConsoleUtils;

public class AbstractTreeVisitor {
	private HashMap<String, Method> methodMap = new HashMap<String, Method>();

	public final Object visit(String method, AbstractTree<?> node) {
		SymbolId tag = node.getTag();
		Method m = findMethod(method, tag);
		if (m != null) {
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
		return visitUndefinedNode(node);
	}

	protected final Method findMethod(String method, SymbolId tag) {
		String key = method + tag.getSymbol();
		Method m = this.methodMap.get(key);
		if (m == null) {
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

	protected Method getClassMethod(String method, SymbolId tag) throws NoSuchMethodException, SecurityException {
		String name = method + tag.getSymbol();
		return this.getClass().getMethod(name, AbstractTree.class);
	}

	protected Object visitUndefinedNode(AbstractTree<?> node) {
		ConsoleUtils.exit(1, "undefined node:" + node);
		return null;
	}

	public final Object visit(String method, Class<?> c1, AbstractTree<?> node, Object p1) {
		SymbolId tag = node.getTag();
		Method m = findMethod(method, tag, c1);
		if (m != null) {
			try {
				return m.invoke(this, node, p1);
			} catch (IllegalAccessException e) {
				Verbose.traceException(e);
			} catch (IllegalArgumentException e) {
				Verbose.traceException(e);
			} catch (InvocationTargetException e) {
				Verbose.traceException(e);
			}
		}
		return visitUndefinedNode(node, p1);
	}

	protected final Method findMethod(String method, SymbolId tag, Class<?> c1) {
		String key = method + tag.getSymbol() + c1.getName();
		Method m = this.methodMap.get(key);
		if (m == null) {
			try {
				m = getClassMethod(method, tag, c1);
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

	protected Method getClassMethod(String method, SymbolId tag, Class<?> c1) throws NoSuchMethodException, SecurityException {
		String name = method + tag.getSymbol();
		return this.getClass().getMethod(name, AbstractTree.class, c1);
	}

	protected Object visitUndefinedNode(AbstractTree<?> node, Object p1) {
		ConsoleUtils.exit(1, "undefined node:" + node + " with " + p1);
		return null;
	}

}
