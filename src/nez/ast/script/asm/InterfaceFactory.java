package nez.ast.script.asm;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import nez.ast.script.Interface;
import nez.ast.script.Reflector;

import org.objectweb.asm.commons.GeneratorAdapter;

public class InterfaceFactory {

	public Interface newConstructor(Type newType, Constructor<?> c) {
		return new IConstructor(newType, c);
	}

}

class IConstructor extends AsmInterface {
	protected Type newType;
	protected Constructor<?> c;

	public IConstructor(Type newType, Constructor<?> c) {
		this.newType = newType;
		this.c = c;
	}

	@Override
	public Type getDeclaringClass() {
		return c.getDeclaringClass();
	}

	@Override
	public Type getReturnType() {
		return newType;
	}

	@Override
	public Type[] getParameterTypes() {
		return c.getGenericParameterTypes();
	}

	@Override
	public org.objectweb.asm.Type getOwner() {
		return getAsmType(c.getDeclaringClass());
	}

	@Override
	public void pushInstruction(GeneratorAdapter a) {
		a.invokeConstructor(getOwner(), getDesc(c));
	}

	@Override
	public String toString() {
		return c.toGenericString();
	}

}

class IPrototype extends Interface {
	String className;
	Type clazz;
	Type returnType;
	Type[] paramTypes;

	IPrototype(Type returnType, Type[] paramTypes) {
		this.returnType = returnType;
		this.paramTypes = paramTypes;
	}

	@Override
	public Type getDeclaringClass() {
		return this.clazz;
	}

	@Override
	public Type getReturnType() {
		return this.returnType;
	}

	@Override
	public Type[] getParameterTypes() {
		return this.paramTypes;
	}

}

class IFunction extends Interface {
	Class<?> funcType;

	@Override
	public Type getDeclaringClass() {
		return null;
	}

	@Override
	public Type getReturnType() {
		Method m = Reflector.findInvokeMethod(funcType);
		return m.getReturnType();
	}

	@Override
	public Type[] getParameterTypes() {
		Method m = Reflector.findInvokeMethod(funcType);
		return m.getParameterTypes();
	}

}

abstract class IMethod extends Interface {
	protected Method method;

	public IMethod(Method m) {
		this.method = m;
	}

}

class ISimpleMethod extends IMethod {

	public ISimpleMethod(Method m) {
		super(m);
	}

	@Override
	public Type getDeclaringClass() {
		return method.getDeclaringClass();
	}

	@Override
	public Type getReturnType() {
		return method.getReturnType();
	}

	@Override
	public Type[] getParameterTypes() {
		return method.getParameterTypes();
	}
}

class IGenericMethod extends IMethod {

	public IGenericMethod(Method m) {
		super(m);
	}

	@Override
	public Type getDeclaringClass() {
		return method.getDeclaringClass();
	}

	@Override
	public Type getReturnType() {
		return method.getGenericReturnType();
	}

	@Override
	public Type[] getParameterTypes() {
		return method.getGenericParameterTypes();
	}
}
