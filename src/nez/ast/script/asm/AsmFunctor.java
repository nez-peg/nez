package nez.ast.script.asm;

import nez.ast.script.Functor;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

public abstract class AsmFunctor extends Functor {

	@Override
	public Object eval(Object recv, Object... args) {
		return null;
	}

	public final Type getAsmType(Class<?> c) {
		return Type.getType(c);
	}

	public Type getOwner() {
		return getAsmType(this.getDeclaringClass());
	}

	public final Method getDesc(java.lang.reflect.Method m) {
		return Method.getMethod(m);
	}

	public final Method getDesc(java.lang.reflect.Constructor<?> c) {
		return Method.getMethod(c);
	}

	public abstract void pushInstruction(GeneratorAdapter a);

}
