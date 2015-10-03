package nez.ast.script;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.HashMap;

public class TypeVarMatcher {

	HashMap<String, Type> vars = new HashMap<>();
	GenericType recv;

	public void init() {
		vars.clear();
	}

	public void init(Type recv2) {
		vars.clear();
		if (recv2 instanceof GenericType) {
			this.recv = (GenericType) recv2;
		}
	}

	public boolean match(Type p, boolean isParam, Type a) {
		if (p instanceof Class<?>) {
			if (!isParam && a instanceof Class<?>) {
				return ((Class<?>) p).isAssignableFrom((Class<?>) a);
			}
			return p == a;
		}
		if (p instanceof TypeVariable) {
			String name = ((TypeVariable<?>) p).getName();
			if (vars.containsKey(name)) {
				return match(vars.get(name), isParam, a);
			}
			if (recv != null) {
				Type t = recv.resolveType(name, null);
				if (t != null) {
					vars.put(name, t);
					return match(t, isParam, a);
				}
			}
			vars.put(name, a);
			return true;
		}
		if (p instanceof ParameterizedType) {
			if (a instanceof GenericType) {
				Type rawtype = ((ParameterizedType) p).getRawType();
				if (!match(rawtype, true, ((GenericType) a).base)) {
					return false;
				}
				Type[] pp = ((ParameterizedType) p).getActualTypeArguments();
				Type[] pa = ((GenericType) a).params;
				if (pp.length != pa.length) {
					return false;
				}
				for (int i = 0; i < pp.length; i++) {
					if (!match(pp[i], true, pa[i])) {
						return false;
					}
				}
				return true;
			}
			System.out.printf("TODO: ParameterizedType %s\n", a.getClass().getName());
			return false;
		}
		if (p instanceof WildcardType) {
			System.out.printf("TODO: WildcardType %s\n", p.getClass().getName());
			return false;
		}
		if (p instanceof GenericArrayType) {
			System.out.printf("TODO: GenericArrayType %s\n", p.getClass().getName());
			return false;
		}
		System.out.printf("TODO: unknown %s\n", p.getClass().getName());
		return false;
	}

	public boolean match(Type p, Type a) {
		return this.match(p, false, a);
	}

	public Type resolve(Type p) {
		if (p instanceof Class<?>) {
			return p;
		}
		if (p instanceof TypeVariable) {
			String name = ((TypeVariable<?>) p).getName();
			if (vars.containsKey(name)) {
				return vars.get(name);
			}
			if (recv != null) {
				Type t = recv.resolveType(name, null);
				if (t != null) {
					vars.put(name, t);
					return t;
				}
			}
			return Object.class; // not found
		}
		if (p instanceof ParameterizedType) {
			Type rawtype = ((ParameterizedType) p).getRawType();
			Type[] params = ((ParameterizedType) p).getActualTypeArguments().clone();
			for (int i = 0; i < params.length; i++) {
				params[i] = resolve(params[i]);
			}
			return GenericType.newType((Class<?>) rawtype, params);
		}
		if (p instanceof WildcardType) {
			System.out.printf("TODO: WildcardType %s\n", p.getClass().getName());
		}
		if (p instanceof GenericArrayType) {
			System.out.printf("TODO: GenericArrayType %s\n", p.getClass().getName());
		}
		System.out.printf("TODO: unknown %s\n", p.getClass().getName());
		return Object.class;
	}

}
