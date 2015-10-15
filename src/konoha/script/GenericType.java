package konoha.script;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;

public class GenericType implements Type {
	int id;
	String name;
	Class<?> base;
	Type[] params;

	private GenericType(int id, Class<?> base, Type... params) {
		this.id = id;
		this.base = base;
		this.params = params;
		this.name = shortName(base, params);
	}

	public Class<?> getRawType() {
		return base;
	}

	public Type resolveType(String name, Type def) {
		int c = 0;
		TypeVariable<?>[] p = base.getTypeParameters();
		for (TypeVariable<?> v : p) {
			if (name.equals(v.getName())) {
				return params[c];
			}
			c++;
		}
		return def;
	}

	public Class<?> resolveClass(String name, Class<?> def) {
		int c = 0;
		TypeVariable<?>[] p = base.getTypeParameters();
		for (TypeVariable<?> v : p) {
			if (name.equals(v.getName())) {
				return TypeSystem.toClass(params[c]);
			}
			c++;
		}
		return def;
	}

	@Override
	public String toString() {
		return this.name;
	}

	public Type resolveType(Type t) {
		if (t instanceof Class<?> || t instanceof GenericType) {
			return t;
		}
		if (t instanceof TypeVariable) {
			return resolveType(((TypeVariable<?>) t).getName(), Object.class);
		}
		if (t instanceof ParameterizedType) {
			Class<?> raw = TypeSystem.toClass(((ParameterizedType) t).getRawType());
			Type[] params = ((ParameterizedType) t).getActualTypeArguments().clone();
			for (int i = 0; i < params.length; i++) {
				params[i] = resolveClass(params[i]);
			}
			return GenericType.newType(raw, params);
		}
		System.out.println("TODO: " + t.getClass() + " as " + t);
		return Object.class;
	}

	public Class<?> resolveClass(Type t) {
		return TypeSystem.toClass(this.resolveType(t));
	}

	static HashMap<String, Type> uniqueMap = new HashMap<>();

	static {
		uniqueMap.put(key(konoha.Array.class, int.class), konoha.IArray.class);
	}

	public final static Type newType(Class<?> base, Type... params) {
		String k = key(base, params);
		Type t = uniqueMap.get(k);
		if (t == null) {
			t = new GenericType(uniqueMap.size(), base, params);
			uniqueMap.put(k, t);
		}
		return t;
	}

	static String key(Class<?> base, Type... params) {
		StringBuilder sb = new StringBuilder();
		sb.append(base.getName());
		for (Type t : params) {
			sb.append(",");
			if (t instanceof Class<?>) {
				sb.append(((Class<?>) t).getName());
			} else if (t instanceof GenericType) {
				sb.append(((GenericType) t).id);
			} else {
				sb.append(t);
			}
		}
		return sb.toString();
	}

	static String shortName(Class<?> base, Type... params) {
		StringBuilder sb = new StringBuilder();
		sb.append(base.getSimpleName());
		sb.append("<");
		int c = 0;
		for (Type t : params) {
			if (c > 0) {
				sb.append(",");
			}
			if (t instanceof Class<?>) {
				sb.append(((Class<?>) t).getSimpleName());
			} else if (t instanceof GenericType) {
				sb.append(((GenericType) t).toString());
			} else {
				sb.append(t);
			}
			sb.append(">");
		}
		return sb.toString();
	}

}
