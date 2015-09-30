package nez.ast.script;

import java.lang.reflect.Method;
import java.util.HashMap;

import nez.ast.Tree;
import nez.util.UList;

public class TypeSystem implements CommonSymbols {
	HashMap<String, Class<?>> nameMap = new HashMap<>();
	UList<Class<?>> classList = new UList<Class<?>>(new Class<?>[4]);

	public TypeSystem() {
		init();
	}

	void init() {
		add(DynamicOperator.class);
		add(StaticOperator.class);
		this.setType("boolean", boolean.class);
		this.setType("int", int.class);
		this.setType("long", long.class);
		this.setType("double", double.class);
		this.setType("String", String.class);
	}

	public void setType(String name, Class<?> type) {
		this.nameMap.put(name, type);
	}

	public final Class<?> resolveType(Tree<?> node, Class<?> deftype) {
		if (node == null) {
			return deftype;
		}
		if (node.size() == 0) {
			Class<?> t = this.nameMap.get(node.toText());
			return t == null ? deftype : t;
		}
		return deftype;
	}

	public boolean declGlobalVariable(String name, Class<?> type) {
		Class<?> t = this.nameMap.get(name);
		this.nameMap.put(name, type);
		return true;
	}

	public Class<?> resolveGlobalVariableType(String name, Class<?> deftype) {
		Class<?> t = this.nameMap.get(name);
		return t == null ? deftype : t;
	}

	void add(Class<?> c) {
		classList.add(c);
	}

	void add(String path) throws ClassNotFoundException {
		add(Class.forName(path));
	}

	public Method findDefaultMethod(String name, int paramsize) {
		for (int i = 0; i < classList.size(); i++) {
			Class<?> c = classList.ArrayValues[i];
			for (Method m : c.getMethods()) {
				if (name.equals(m.getName()) && m.getParameterTypes().length == paramsize) {
					return m;
				}
			}
		}
		return null;
	}

	public TypedTree enforceType(Class<?> req, TypedTree node) {
		if (accept(false, req, node.getType())) {
			return node;
		}
		;
		System.out.printf("TODO: needs cast %s %s\n", req, node.getType());
		return node;
	}

	public Method findCompiledMethod(String name, Class<?>... args) {
		for (int i = classList.size() - 1; i >= 0; i--) {
			Class<?> c = classList.ArrayValues[i];
			for (Method m : c.getMethods()) {
				if (!name.equals(m.getName())) {
					continue;
				}
				if (acceptArguments(true, m, args)) {
					return m;
				}
			}
		}
		return null;
	}

	boolean acceptArguments(boolean autoBoxing, Method m, Class<?>... args) {
		Class<?>[] p = m.getParameterTypes();
		if (args.length != p.length) {
			return false;
		}
		for (int j = 0; j < args.length; j++) {
			if (!accept(autoBoxing, p[j], args[j])) {
				return false;
			}
		}
		return true;
	}

	boolean accept(boolean autoBoxing, Class<?> p, Class<?> a) {
		if (a == null) {
			return true;
		}
		if (autoBoxing) {
			if (p == int.class && a == Integer.class) {
				return true;
			}
			if (p == double.class && a == Double.class) {
				return true;
			}
		}
		// System.out.printf("%s %s %s\n", p, a, p.isAssignableFrom(a));
		if (p.isAssignableFrom(a)) {
			return true;
		}
		return false;
	}

	// typeof

	public Class<?> typeof(Tree<?> node) {
		if (node instanceof TypedTree) {
			Class<?> type = ((TypedTree) node).type;
			if (type != null) {
				return type;
			}
		}
		return Object.class; // untyped
	}

}