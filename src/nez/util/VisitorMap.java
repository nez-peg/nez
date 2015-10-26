package nez.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

public class VisitorMap<V> {
	protected V defaultAcceptor;
	protected HashMap<String, V> visitors;

	protected void init(Class<?> baseClass, V defualtAccepter) {
		this.defaultAcceptor = defualtAccepter;
		this.visitors = new HashMap<>();
		for (Class<?> c : baseClass.getClasses()) {
			load(c);
		}
	}

	@SuppressWarnings("unchecked")
	private void load(Class<?> c) {
		try {
			Constructor<?> cc = c.getConstructor(this.getClass());
			Object v = cc.newInstance(this);
			if (check(defaultAcceptor.getClass(), v.getClass())) {
				// System.out.println("c: " + c.getSimpleName());
				String n = c.getSimpleName();
				if (n.startsWith("_")) {
					n = n.substring(1);
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
}
