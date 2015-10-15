package konoha.script;

import java.lang.reflect.Type;

public abstract class Functor {

	public abstract String getName();

	public abstract Class<?> getReturnClass();

	public abstract Type getReturnType();

	public abstract Type[] getParameterTypes();

	public abstract Object eval(Object recv, Object... args);

	public final static boolean match(TypeMatcher matcher, Type[] p, TypedTree params) {
		if (p.length != params.size()) {
			return false;
		}
		for (int j = 0; j < p.length; j++) {
			if (!match(matcher, p[j], params.get(j).getType())) {
				return false;
			}
		}
		return true;
	}

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
