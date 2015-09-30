package nez.ast.script;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;

import nez.util.IArray;
import nez.util.UList;

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

	public Type resolve(String name, Type def) {
		int c = 0;
		TypeVariable<?>[] p = base.getTypeParameters();
		for (TypeVariable<?> v : p) {
			if (name.equals(v.getName())) {
				return params[c];
			}
			c++;
		}
		return null;
	}

	@Override
	public String toString() {
		return this.name;
	}

	public Type resolve(Type t) {
		if (t instanceof Class<?> || t instanceof GenericType) {
			return t;
		}
		if (t instanceof TypeVariable) {
			return resolve(((TypeVariable<?>) t).getName(), Object.class);
		}
		if (t instanceof ParameterizedType) {
			Type raw = resolve(((ParameterizedType) t).getRawType());
			Type[] params = ((ParameterizedType) t).getActualTypeArguments().clone();
			for (int i = 0; i < params.length; i++) {
				params[i] = resolve(params[i]);
			}
			return GenericType.newType((Class<?>) raw, params);
		}
		System.out.println("TODO: " + t.getClass() + " as " + t);
		return Object.class;
	}

	static HashMap<String, Type> uniqueMap = new HashMap<>();

	static {
		// int[]
		// uniqueMap.put(key(UList.class, int.class), new
		// GenericType(uniqueMap.size(), IArray.class, int.class));
		uniqueMap.put(key(UList.class, int.class), IArray.class);

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
