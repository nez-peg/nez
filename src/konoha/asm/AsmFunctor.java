package konoha.asm;

import konoha.script.Functor;
import konoha.script.TypeSystem;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

public abstract class AsmFunctor extends Functor {

	public final Type newAsmType(Class<?> c) {
		return Type.getType(c);
	}

	public final Type newAsmType(java.lang.reflect.Type t) {
		return Type.getType(TypeSystem.toClass(t));
	}

	public abstract Type getOwner();

	public Type getOwner(Class<?> c) {
		return newAsmType(c);
	}

	public final Method getDesc(java.lang.reflect.Method m) {
		return Method.getMethod(m);
	}

	public final Method getDesc(java.lang.reflect.Constructor<?> c) {
		return Method.getMethod(c);
	}

	public final Method getDesc(java.lang.reflect.Type returnType, String name, java.lang.reflect.Type[] paramTypes) {
		Type[] paramTypeDescs = new Type[paramTypes.length];
		for (int i = 0; i < paramTypes.length; i++) {
			paramTypeDescs[i] = newAsmType(paramTypes[i]);
		}
		return new Method(name, newAsmType(returnType), paramTypeDescs);
	}

	public final Method getDesc(String name) {
		return this.getDesc(this.getReturnType(), name, this.getParameterTypes());
	}

	public abstract void pushInstruction(GeneratorAdapter a);

}

abstract class AsmSymbolFunctor extends AsmFunctor {
	protected String cname;

	protected AsmSymbolFunctor(String cname) {
		this.cname = cname;
	}

	@Override
	public Type getOwner() {
		return Type.getType("L" + this.cname + ";");
	}
}
