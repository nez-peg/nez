package nez.ast.script;

import java.lang.reflect.Method;

import konoha.Function;
import nez.ast.script.asm.InterfaceFactory;

public class KonohaRuntime {
	private Interface assert_ = null;

	Interface System_assert(InterfaceFactory factory) {
		if (assert_ == null) {
			assert_ = factory.newMethod(Reflector.load(this.getClass(), "assert_", boolean.class, String.class));
		}
		return assert_;
	}

	public final static void assert_(boolean cond, String msg) {
		assert (cond) : msg;
	}

	private static Interface StaticErrorMethod = null;

	Interface error(InterfaceFactory factory) {
		if (StaticErrorMethod == null) {
			StaticErrorMethod = factory.newMethod(Reflector.load(this.getClass(), "error", String.class));
		}
		return StaticErrorMethod;
	}

	public final static void error(String msg) {
		throw new ScriptRuntimeException(msg);
	}

	private Interface String_join = null;

	Interface String_join(InterfaceFactory factory) {
		if (String_join == null) {
			String_join = factory.newMethod(Reflector.load(this.getClass(), "String_join", Object[].class));
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

	private Interface Object_invokeDynamic = null;

	Interface Object_invokeDynamic(InterfaceFactory factory) {
		if (Object_invokeDynamic == null) {
			Object_invokeDynamic = factory.newMethod(Reflector.load(this.getClass(), "Object_invokeDynamic", Object.class, String.class, Object[].class));
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

	private Interface Object_getField = null;

	Interface Object_getField(InterfaceFactory factory) {
		if (Object_getField == null) {
			Object_getField = factory.newMethod(Reflector.load(this.getClass(), "Object_getField", Object.class, String.class));
		}
		return Object_getField;
	}

	public final static Object Object_getField(Object self, String name) {
		return Reflector.getField(self, name);
	}

	private Interface Object_setField = null;

	Interface Object_setField(InterfaceFactory factory) {
		if (Object_setField == null) {
			Object_setField = factory.newMethod(Reflector.load(this.getClass(), "Object_setField", Object.class, String.class, Object.class));
		}
		return Object_setField;
	}

	public final static Object Object_setField(Object self, String name, Object val) {
		Reflector.setField(self, name, val);
		return val;
	}

	private Interface Object_invokeFunction = null;

	Interface Object_invokeFunction(InterfaceFactory factory) {
		if (Object_invokeFunction == null) {
			Object_invokeFunction = factory.newMethod(Reflector.load(this.getClass(), "Object_invokeFunction", Function.class, Object[].class));
		}
		return Object_invokeFunction;
	}

	public final static Object invokeFunc(Function self, Object... a) {
		return Reflector.invokeMethod(self, self.f, a);
	}

}
