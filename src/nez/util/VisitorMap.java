package nez.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

import nez.ast.UndefinedVisitorMapException;
import nez.ast.Tree;

public class VisitorMap<V> {
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
			System.out.println(e.getMessage());
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
		throw new UndefinedVisitorMapException(node, this.getClass().getName() + ": undefined " + node);
	}

}
