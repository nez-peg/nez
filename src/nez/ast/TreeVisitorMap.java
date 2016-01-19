package nez.ast;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

import nez.util.Verbose;

public class TreeVisitorMap<V> {
	private static boolean OnWhenDebugging = false;

	protected V defaultAcceptor;
	protected HashMap<String, V> visitors;

	protected void init(Class<?> baseClass, V defualtAccepter) {
		this.defaultAcceptor = defualtAccepter;
		this.visitors = new HashMap<>();
		visitors.put(defualtAccepter.getClass().getSimpleName(), defaultAcceptor);
		if (OnWhenDebugging) {
			System.out.println("base: " + baseClass);
		}
		for (Class<?> c : baseClass.getClasses()) {
			load(baseClass, c);
		}
	}

	@SuppressWarnings("unchecked")
	private void load(Class<?> baseClass, Class<?> c) {
		try {
			Constructor<?> cc = c.getConstructor(baseClass);
			Object v = cc.newInstance(this);
			if (check(defaultAcceptor.getClass(), v.getClass())) {
				String n = c.getSimpleName();
				if (n.startsWith("_")) {
					n = n.substring(1);
				}
				if (OnWhenDebugging) {
					System.out.println(" #" + n);
				}
				visitors.put(n, (V) v);
			}
		} catch (NoSuchMethodException | SecurityException | InvocationTargetException | IllegalAccessException | InstantiationException | IllegalArgumentException e) {
			Verbose.println("TreeVisitorMap.load(%s, %s): %s", baseClass.getName(), c.getName(), e.toString());
			// Verbose.traceException(e);
		}
	}

	private boolean check(Class<?> v, Class<?> e) {
		return v.isAssignableFrom(e);
	}

	public final void add(String name, V visitor) {
		visitors.put(name, visitor);
	}

	protected final V find(String name) {
		V v = visitors.get(name);
		return v == null ? defaultAcceptor : v;
	}

	protected final void undefined(Tree<?> node) {
		if (OnWhenDebugging) {
			System.out.println("undefined: " + node);
		}
		throw new UndefinedException(node, this.getClass().getName() + ": undefined " + node);
	}

	@SuppressWarnings("serial")
	public static class UndefinedException extends RuntimeException {
		Tree<?> node;

		public UndefinedException(Tree<?> node, String msg) {
			super(node.formatSourceMessage("error", msg));
			this.node = node;
		}

		public UndefinedException(Tree<?> node, String fmt, Object... args) {
			super(node.formatSourceMessage("error", String.format(fmt, args)));
			this.node = node;
		}
	}
}
