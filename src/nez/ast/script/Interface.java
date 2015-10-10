package nez.ast.script;

import java.lang.reflect.Type;

public abstract class Interface {
	public abstract Class<?> getDeclaringClass();

	public abstract Class<?> getReturnClass();

	public abstract Type getReturnType();

	public abstract Type[] getParameterTypes();

	public abstract Object eval(Object recv, Object... args);

	// public boolean match(TypeSystem ts, TypeVarMatcher matcher, TypedTree
	// params, TypedTree[] results) {
	// Type[] p = this.getParameterTypes();
	// if (results == null && p.length > 0) {
	// results = new TypedTree[p.length];
	// }
	// for (int i = 0; i < p.length; i++) {
	// TypedTree sub = params.get(i);
	// results[i] = this.typeCheck(ts, matcher, p[i], sub);
	// if (results[i] == null) {
	// if (matcher != null) {
	// matcher.reset();
	// }
	// return false;
	// }
	// }
	// for (int i = 0; i < p.length; i++) {
	// params.set(i, results[i]);
	// }
	// return true;
	// }

	public final static boolean match(TypeMatcher matcher, Type[] p, TypedTree params) {
		if (p.length != params.size()) {
			return false;
		}
		for (int j = 0; j < p.length; j++) {
			if (!match(matcher, p[j], params.getType())) {
				return false;
			}
		}
		return true;
	}

	//
	// public final static boolean match(TypeVarMatcher matcher, Type[] p,
	// Type[] types) {
	// if (p.length != types.length) {
	// return false;
	// }
	// for (int j = 0; j < types.length; j++) {
	// if (!match(matcher, p[j], types[j])) {
	// return false;
	// }
	// }
	// return true;
	// }

	public final static boolean match(TypeMatcher matcher, Type p, Type a) {
		if (p == a) {
			return true;
		}
		if (p instanceof Class<?> || matcher == null) {
			if (((Class<?>) p).isAssignableFrom(TypeSystem.toClass(a))) {
				return true;
			}
			return false;
		}
		return matcher.match(p, a);
	}
}
