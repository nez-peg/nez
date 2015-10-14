package nez.ast.script;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import konoha.Function;
import nez.util.ConsoleUtils;

public class Reflector {

	public static Object newInstance(Class<?> c) {
		try {
			return c.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new ScriptRuntimeException(e.getMessage());
		}
	}

	public static Object newInstance(Constructor<?> c, Object[] args) {
		try {
			return c.newInstance(args);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new ScriptRuntimeException(e.toString());
		}
	}

	public final static Method load(Class<?> c, String name, Class<?>... type) {
		try {
			return c.getMethod(name, type);
		} catch (NoSuchMethodException | SecurityException e) {
			ConsoleUtils.exit(1, e.getMessage());
			return null;
		}
	}

	public final static Method getMethod(Object self, String name, Class<?>... type) {
		try {
			return self.getClass().getMethod(name, type);
		} catch (NoSuchMethodException | SecurityException e) {
			throw new ScriptRuntimeException(e.getMessage());
		}
	}

	public final static Method getNullableMethod(Object self, String name, Class<?>... type) {
		try {
			return self.getClass().getMethod(name, type);
		} catch (NoSuchMethodException | SecurityException e) {
		}
		return null;
	}

	public final static Object getStatic(Field f) {
		try {
			return f.get(null);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new ScriptRuntimeException(e.getMessage());
		}
	}

	public final static Object getStatic(Field f, Object defval) {
		try {
			return f.get(null);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			if (defval == null) {
				throw new ScriptRuntimeException(e.getMessage());
			}
		}
		return defval;
	}

	public final static void setStatic(Field f, Object val) {
		try {
			f.set(null, val);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new ScriptRuntimeException(e.getMessage());
		}
	}

	public final static Object getField(Object self, Field f) {
		try {
			return f.get(self);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new ScriptRuntimeException(e.getMessage());
		}
	}

	public final static Object getField(Object self, String name) {
		try {
			Field f = self.getClass().getField(name);
			return getField(self, f);
		} catch (NoSuchFieldException | SecurityException e) {
			throw new ScriptRuntimeException(e.getMessage());
		}
	}

	public final static Object getField(Object self, Field f, Object defval) {
		try {
			return f.get(self);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			if (defval == null) {
				throw new ScriptRuntimeException(e.getMessage());
			}
		}
		return defval;
	}

	public final static void setField(Object self, Field f, Object val) {
		try {
			f.set(self, val);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new ScriptRuntimeException(e.getMessage());
		}
	}

	public final static void setField(Object self, String name, Object val) {
		try {
			Field f = self.getClass().getField(name);
			setField(self, f, val);
		} catch (NoSuchFieldException | SecurityException e) {
			throw new ScriptRuntimeException(e.getMessage());
		}
	}

	public final static Object invokeStaticMethod(Method m, Object... args) {
		return invokeMethod(null, m, args);
	}

	public final static Object invokeMethod(Object self, Method m, Object... args) {
		try {
			return m.invoke(self, args);
		} catch (IllegalAccessException | IllegalArgumentException e) {
			throw new ScriptRuntimeException("" + m + " caused by " + e.toString());
		} catch (InvocationTargetException e) {
			Throwable w = e.getTargetException();
			if (w instanceof RuntimeException) {
				throw (RuntimeException) e.getTargetException();
			}
			// e.printStackTrace();
			throw new ScriptRuntimeException("" + m + " caused by " + w.toString());
		}
	}

	public final static Class<?> infer(Object value) {
		if (value instanceof Number) {
			if (value instanceof Integer) {
				return int.class;
			}
			if (value instanceof Double) {
				return double.class;
			}
			if (value instanceof Long) {
				return long.class;
			}
			if (value instanceof Short) {
				return short.class;
			}
			if (value instanceof Character) {
				return char.class;
			}
			if (value instanceof Byte) {
				return byte.class;
			}
		}
		if (value instanceof Boolean) {
			return boolean.class;
		}
		return value == null ? Object.class : value.getClass();
	}

	/* utils */

	public final static Object invokeDynamic(Object self, String name) {
		Method m = Reflector.getMethod(self, name);
		return Reflector.invokeMethod(self, m);
	}

	public final static Object invokeDynamic(Object self, String name, Object a1) {
		Method m = Reflector.getMethod(self, name, infer(a1));
		return Reflector.invokeMethod(self, m, a1);
	}

	public final static Object invokeDynamic(Object self, String name, Object a1, Object a2) {
		Method m = Reflector.getMethod(self, name, infer(a1), infer(a2));
		return Reflector.invokeMethod(self, m, a1, a2);
	}

	public final static Object invokeDynamic(Object self, String name, Object a1, Object a2, Object a3) {
		Method m = Reflector.getMethod(self, name, infer(a1), infer(a2), infer(a3));
		return Reflector.invokeMethod(self, m, a1, a2, a3);
	}

	static Method[] invokeDynamicMethods = null;

	public final static Method getInvokeDynamicMethod(int paramsize) {
		if (invokeDynamicMethods == null) {
			invokeDynamicMethods = new Method[4];
			invokeDynamicMethods[0] = load(Reflector.class, "invokeDynamic", Object.class, String.class);
			invokeDynamicMethods[1] = load(Reflector.class, "invokeDynamic", Object.class, String.class, Object.class);
			invokeDynamicMethods[2] = load(Reflector.class, "invokeDynamic", Object.class, String.class, Object.class, Object.class);
			invokeDynamicMethods[3] = load(Reflector.class, "invokeDynamic", Object.class, String.class, Object.class, Object.class, Object.class);
		}
		if (paramsize < invokeDynamicMethods.length) {
			return invokeDynamicMethods[paramsize];
		}
		return null;
	}

	/* function */

	public final static Object invokeFunc(Function self, Object... args) {
		return Reflector.invokeMethod(self, self.f, args);
	}

	public final static Object invokeFunc(Function self) {
		return Reflector.invokeMethod(self, self.f);
	}

	public final static Object invokeFunc(Function self, Object a1) {
		return Reflector.invokeMethod(self, self.f, a1);
	}

	public final static Object invokeFunc(Function self, Object a1, Object a2) {
		return Reflector.invokeMethod(self, self.f, a1, a2);
	}

	public final static Object invokeFunc(Function self, Object a1, Object a2, Object a3) {
		return Reflector.invokeMethod(self, self.f, a1, a2, a3);
	}

	static Method[] invokeFuncMethods = null;

	public final static Method getInvokeFunctionMethod(int paramsize) {
		if (invokeFuncMethods == null) {
			invokeFuncMethods = new Method[4];
			invokeFuncMethods[0] = load(Reflector.class, "invokeFunc", Function.class);
			invokeFuncMethods[1] = load(Reflector.class, "invokeFunc", Function.class, Object.class);
			invokeFuncMethods[2] = load(Reflector.class, "invokeFunc", Function.class, Object.class, Object.class);
			invokeFuncMethods[3] = load(Reflector.class, "invokeFunc", Function.class, Object.class, Object.class, Object.class);
		}
		if (paramsize < invokeFuncMethods.length) {
			return invokeFuncMethods[paramsize];
		}
		return null;
	}

	public final static Method findInvokeMethod(Class<?> f) {
		return f.getDeclaredMethods()[0];
	}

	public final static Method findInvokeMethod(Function self) {
		return self.getClass().getDeclaredMethods()[0];
	}

}
