package nez.ast.script;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

public class GenericType implements Type {
	Class<?> base;
	Type[] params;

	public GenericType(Class<?> base, Type... params) {
		this.base = base;
		this.params = params;
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
}
