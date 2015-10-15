package konoha.script;

import java.lang.reflect.Method;

import konoha.Function;
import konoha.asm.FunctorFactory;

public abstract class KonohaRuntime {
	static private Functor assert_ = null;

	static Functor System_assert() {
		if (assert_ == null) {
			assert_ = FunctorFactory.newMethod(Reflector.load(KonohaRuntime.class, "assert_", boolean.class, String.class));
		}
		return assert_;
	}

	public final static void assert_(boolean cond, String msg) {
		assert (cond) : msg;
	}

	private static Functor StaticErrorMethod = null;

	static Functor error() {
		if (StaticErrorMethod == null) {
			StaticErrorMethod = FunctorFactory.newMethod(Reflector.load(KonohaRuntime.class, "error", String.class));
		}
		return StaticErrorMethod;
	}

	public final static void error(String msg) {
		throw new ScriptRuntimeException(msg);
	}

	private static Functor String_join = null;

	static Functor String_join() {
		if (String_join == null) {
			String_join = FunctorFactory.newMethod(Reflector.load(KonohaRuntime.class, "String_join", Object[].class));
		}
		return String_join;
	}

	public final static String String_join(Object[] args) {
		StringBuilder sb = new StringBuilder();
		for (Object a : args) {
			if (!(a instanceof Object[])) {
				sb.append(a);
			}
		}
		return sb.toString();
	}

	private static Functor Object_invokeDynamic = null;

	static Functor Object_invokeDynamic() {
		if (Object_invokeDynamic == null) {
			Object_invokeDynamic = FunctorFactory.newMethod(Reflector.load(KonohaRuntime.class, "Object_invokeDynamic", Object.class, String.class, Object[].class));
		}
		return Object_invokeDynamic;
	}

	public final static Object Object_invokeDynamic(Object self, String name, Object... args) {
		Class<?>[] p = new Class<?>[args.length];
		for (int i = 0; i < args.length; i++) {
			p[i] = Reflector.infer(args[i]);
		}
		Method m = Reflector.getMethod(self, name, p);
		return Reflector.invokeMethod(self, m, args);
	}

	private static Functor Object_getField = null;

	static Functor Object_getField() {
		if (Object_getField == null) {
			Object_getField = FunctorFactory.newMethod(Reflector.load(KonohaRuntime.class, "Object_getField", Object.class, String.class));
		}
		return Object_getField;
	}

	public final static Object Object_getField(Object self, String name) {
		return Reflector.getField(self, name);
	}

	private static Functor Object_setField = null;

	static Functor Object_setField() {
		if (Object_setField == null) {
			Object_setField = FunctorFactory.newMethod(Reflector.load(KonohaRuntime.class, "Object_setField", Object.class, String.class, Object.class));
		}
		return Object_setField;
	}

	public final static Object Object_setField(Object self, String name, Object val) {
		Reflector.setField(self, name, val);
		return val;
	}

	private static Functor Object_invokeFunction = null;

	static Functor Object_invokeFunction() {
		if (Object_invokeFunction == null) {
			Object_invokeFunction = FunctorFactory.newMethod(Reflector.load(KonohaRuntime.class, "Object_invokeFunction", Function.class, Object[].class));
		}
		return Object_invokeFunction;
	}

	public final static Object invokeFunc(Function self, Object... a) {
		return Reflector.invokeMethod(self, self.f, a);
	}

}
