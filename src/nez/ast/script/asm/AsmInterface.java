package nez.ast.script.asm;

import nez.ast.script.Interface;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

public abstract class AsmInterface extends Interface {

	public final Type getAsmType(Class<?> c) {
		return Type.getType(c);
	}

	public final Method getDesc(java.lang.reflect.Method m) {
		return Method.getMethod(m);
	}

	public final Method getDesc(java.lang.reflect.Constructor<?> c) {
		return Method.getMethod(c);
	}

	public abstract Type getOwner();

	public abstract void pushInstruction(GeneratorAdapter a);
}
