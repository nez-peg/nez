package nez.ast.script;

import java.lang.reflect.Method;

import nez.util.UList;

public class ClassRepository {
	UList<Class<?>> classList = new UList<Class<?>>(new Class<?>[4]);

	public ClassRepository() {
		add(DynamicOperator.class);
		add(StaticOperator.class);
	}

	void add(Class<?> c) {
		classList.add(c);
	}

	Method findMethod(String name, Class<?>... args) {
		for (int i = classList.size() - 1; i >= 0; i--) {
			Class<?> c = classList.ArrayValues[i];
			for (Method m : c.getMethods()) {
				if (!name.equals(m.getName())) {
					continue;
				}
				Class<?>[] p = m.getParameterTypes();
				if (args.length != p.length) {
					continue;
				}
				for (int j = 0; j < args.length; j++) {
					if (!accept(p[j], args[j])) {
						continue;
					}
				}
				return m;
			}
		}
		return null;
	}

	boolean accept(Class<?> p, Class<?> a) {
		if (a == null) {
			return true;
		}
		System.out.printf("%s %s %s\n", p, a, p.isAssignableFrom(a));
		return p.isAssignableFrom(a);
	}

}