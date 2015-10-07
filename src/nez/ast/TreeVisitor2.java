package nez.ast;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

public class TreeVisitor2<V> {
	protected V undefined;
	protected HashMap<String, V> visitors;

	protected void init(V undefined) {
		this.undefined = undefined;
		this.visitors = new HashMap<>();
		for (Class<?> c : this.getClass().getClasses()) {
			load(c);
		}
	}

	@SuppressWarnings("unchecked")
	private void load(Class<?> c) {
		try {
			Constructor<?> cc = c.getConstructor(this.getClass());
			Object v = cc.newInstance(this);
			if (check(undefined.getClass(), v.getClass())) {
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

	protected final V find(Tree<?> node) {
		V v = visitors.get(node.getTag().toString());
		return v == null ? undefined : v;
	}
}
