package konoha.script;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.HashMap;

import nez.util.UList;

public class TypeMatcher {
	TypeSystem typeSystem;
	TypeChecker typeChecker;
	UList<Functor> candiateList = new UList<Functor>(new Functor[32]);
	String mismatched;

	HashMap<String, Type> vars = new HashMap<>();
	GenericType recvType;

	TypeMatcher(TypeSystem typeSystem, TypeChecker typeChecker) {
		this.typeSystem = typeSystem;
		this.typeChecker = typeChecker;
	}

	public final void init(Type recvType) {
		if (recvType instanceof GenericType) {
			this.recvType = (GenericType) recvType;
		} else {
			this.recvType = null;
		}
		vars.clear();
		this.candiateList.clear(0);
	}

	public final void addCandidate(Functor inf) {
		if (inf != null) {
			this.candiateList.add(inf);
		}
	}

	public final String getErrorMessage() {
		mismatched = null;
		if (this.candiateList.size() > 0) {
			StringBuilder sb = new StringBuilder();
			for (Functor inf : this.candiateList) {
				sb.append(" ");
				sb.append(inf.toString());
			}
			mismatched = sb.toString();
		}
		return mismatched;
	}

	public final boolean match(Type p, Type a) {
		return this.match(false, p, a);
	}

	public final boolean match(boolean isTypeParameter, Type p, Type a) {
		if (p instanceof Class<?>) {
			if (!isTypeParameter && a instanceof Class<?>) {
				return ((Class<?>) p).isAssignableFrom((Class<?>) a);
			}
			return p == a;
		}
		if (p instanceof TypeVariable) {
			String name = ((TypeVariable<?>) p).getName();
			if (vars.containsKey(name)) {
				return match(isTypeParameter, vars.get(name), a);
			}
			if (recvType != null) {
				Type t = recvType.resolveType(name, null);
				if (t != null) {
					vars.put(name, t);
					return match(isTypeParameter, t, a);
				}
			}
			vars.put(name, a);
			return true;
		}
		if (p instanceof ParameterizedType) {
			if (a instanceof GenericType) {
				Type rawtype = ((ParameterizedType) p).getRawType();
				if (!match(true, rawtype, ((GenericType) a).base)) {
					return false;
				}
				Type[] pp = ((ParameterizedType) p).getActualTypeArguments();
				Type[] pa = ((GenericType) a).params;
				if (pp.length != pa.length) {
					return false;
				}
				for (int i = 0; i < pp.length; i++) {
					if (!match(true, pp[i], pa[i])) {
						return false;
					}
				}
				return true;
			}
			typeSystem.TODO("ParameterizedType %s\n", a.getClass().getName());
			return false;
		}
		if (p instanceof WildcardType) {
			typeSystem.TODO("WildcardType %s\n", p.getClass().getName());
			return false;
		}
		if (p instanceof GenericArrayType) {
			typeSystem.TODO("GenericArrayType %s\n", p.getClass().getName());
			return false;
		}
		typeSystem.TODO("unknown %s\n", p.getClass().getName());
		return false;
	}

	public final Type resolve(Type p, Class<?> unresolved) {
		if (p instanceof Class<?>) {
			return p;
		}
		if (p instanceof TypeVariable) {
			String name = ((TypeVariable<?>) p).getName();
			if (vars.containsKey(name)) {
				return vars.get(name);
			}
			if (recvType != null) {
				Type t = recvType.resolveType(name, null);
				if (t != null) {
					vars.put(name, t);
					return t;
				}
			}
			return unresolved; // not found
		}
		if (p instanceof ParameterizedType) {
			Type rawtype = ((ParameterizedType) p).getRawType();
			Type[] params = ((ParameterizedType) p).getActualTypeArguments().clone();
			for (int i = 0; i < params.length; i++) {
				params[i] = resolve(params[i], Object.class);
			}
			return GenericType.newType((Class<?>) rawtype, params);
		}
		if (p instanceof WildcardType) {
			typeSystem.TODO("WildcardType %s\n", p.getClass().getName());
		}
		if (p instanceof GenericArrayType) {
			typeSystem.TODO("GenericArrayType %s\n", p.getClass().getName());
		}
		typeSystem.TODO("unknown %s\n", p.getClass().getName());
		return unresolved;
	}

}
