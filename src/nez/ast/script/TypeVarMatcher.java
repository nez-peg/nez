package nez.ast.script;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.HashMap;

import nez.util.UList;

public class TypeVarMatcher {
	TypeSystem typeSystem;
	UList<Interface> candiateList = new UList<Interface>(new Interface[32]);
	String mismatched;
	HashMap<String, Type> vars = new HashMap<>();
	GenericType recvType;

	TypeVarMatcher(TypeSystem typeSystem) {
		this.typeSystem = typeSystem;
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

	public final void reset() {
		vars.clear();
	}

	public final void addCandidate(Interface inf) {
		if (inf != null) {
			this.candiateList.add(inf);
		}
	}

	public final Interface matchCandidate(TypedTree params) {
		TypedTree[] buf = null;
		for (Interface inf : this.candiateList) {
			if (buf == null) {
				buf = new TypedTree[params.size()];
			}
			if (inf.match(typeSystem, this, params, buf)) {
				return inf;
			}
		}
		buf = null;
		mismatched = null;
		if (this.candiateList.size() > 0) {
			StringBuilder sb = new StringBuilder();
			for (Interface inf : this.candiateList) {
				sb.append(" ");
				sb.append(inf.toString());
			}
			mismatched = sb.toString();
		}
		return null;
	}

	public final String getErrorMessage() {
		return mismatched;
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
			if (recvType != null) {
				Type t = recvType.resolveType(name, null);
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

	public boolean match(Type p, Type a) {
		return this.match(p, false, a);
	}

	public Type resolve(Type p, Class<?> unresolved) {
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
