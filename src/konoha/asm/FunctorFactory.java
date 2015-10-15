package konoha.asm;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;

import konoha.Function;
import konoha.script.Functor;
import konoha.script.Reflector;
import konoha.script.TypeSystem;

import org.objectweb.asm.commons.GeneratorAdapter;

public abstract class FunctorFactory {

	public final static Functor newConstructor(Type newType, Constructor<?> c) {
		return new ConstructorFunctor(newType, c);
	}

	public final static Functor newMethod(Method m) {
		if (Modifier.isStatic(m.getModifiers())) {
			return new StaticMethodFunctor(m);
		}
		if (Modifier.isInterface(m.getModifiers())) {
			return new InterfaceFunctor(m);
		}
		return new MethodFunctor(m);
	}

}

class ConstructorFunctor extends AsmFunctor {
	protected Type newType;
	protected Constructor<?> c;

	public ConstructorFunctor(Type newType, Constructor<?> c) {
		this.newType = newType;
		this.c = c;
	}

	@Override
	public String getName() {
		return "<init>";
	}

	@Override
	public Class<?> getReturnClass() {
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
	public Object eval(Object recv, Object... args) {
		return Reflector.newInstance(c, args);
	}

	@Override
	public org.objectweb.asm.Type getOwner() {
		return getOwner(c.getDeclaringClass());
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

class MethodFunctor extends AsmFunctor {
	protected Method method;

	public MethodFunctor(Method method) {
		this.method = method;
	}

	@Override
	public String getName() {
		return "";
	}

	@Override
	public Class<?> getReturnClass() {
		return this.method.getReturnType();
	}

	@Override
	public Type getReturnType() {
		return this.method.getGenericReturnType();
	}

	@Override
	public Type[] getParameterTypes() {
		return method.getGenericParameterTypes();
	}

	@Override
	public Object eval(Object recv, Object... args) {
		return Reflector.invokeMethod(recv, method, args);
	}

	@Override
	public org.objectweb.asm.Type getOwner() {
		return getOwner(this.method.getDeclaringClass());
	}

	@Override
	public void pushInstruction(GeneratorAdapter a) {
		a.invokeVirtual(this.getOwner(), this.getDesc(method));
	}

	@Override
	public String toString() {
		return method.toGenericString();
	}
}

class StaticMethodFunctor extends MethodFunctor {
	public StaticMethodFunctor(Method method) {
		super(method);
	}

	@Override
	public Object eval(Object recv, Object... args) {
		return Reflector.invokeMethod(null, method, args);
	}

	@Override
	public void pushInstruction(GeneratorAdapter a) {
		a.invokeStatic(this.getOwner(method.getDeclaringClass()), this.getDesc(method));
	}
}

class InterfaceFunctor extends MethodFunctor {
	public InterfaceFunctor(Method method) {
		super(method);
	}

	@Override
	public void pushInstruction(GeneratorAdapter a) {
		a.invokeInterface(this.getOwner(method.getDeclaringClass()), this.getDesc(method));
	}
}

class SymbolFunctor extends AsmSymbolFunctor {

	Type returnType;
	String name;
	Type[] paramTypes;

	protected SymbolFunctor(String cname, Type returnType, String name, Type[] paramTypes) {
		super(cname);
		this.returnType = returnType;
		this.name = name;
		this.paramTypes = paramTypes;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Class<?> getReturnClass() {
		return TypeSystem.toClass(this.returnType);
	}

	@Override
	public Type getReturnType() {
		return this.returnType;
	}

	@Override
	public Type[] getParameterTypes() {
		return this.paramTypes;
	}

	@Override
	public Object eval(Object recv, Object... args) {
		return null;
	}

	@Override
	public void pushInstruction(GeneratorAdapter a) {
		a.invokeStatic(this.getOwner(null), this.getDesc(this.name));
	}

}

class FunctionFunctor extends AsmFunctor {
	String name;
	Type funcType;

	FunctionFunctor(String name, Type funcType) {
		this.name = name;
		this.funcType = funcType;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public Class<?> getReturnClass() {
		return TypeSystem.getFuncReturnType(funcType);
	}

	@Override
	public Type getReturnType() {
		return TypeSystem.getFuncReturnType(funcType);
	}

	@Override
	public Type[] getParameterTypes() {
		return TypeSystem.getFuncParameterTypes(funcType);
	}

	@Override
	public org.objectweb.asm.Type getOwner() {
		return newAsmType(funcType);
	}

	@Override
	public Object eval(Object recv, Object... args) {
		return Reflector.invokeFunc((Function) recv, args);
	}

	@Override
	public void pushInstruction(GeneratorAdapter a) {
		a.invokeVirtual(newAsmType(funcType), getDesc("invoke"));
	}

}

// class IPrototype extends Interface {
// String className;
// Type clazz;
// Type returnType;
// Type[] paramTypes;
//
// IPrototype(Type returnType, Type[] paramTypes) {
// this.returnType = returnType;
// this.paramTypes = paramTypes;
// }
//
// @Override
// public Type getDeclaringClass() {
// return this.clazz;
// }
//
// @Override
// public Type getReturnType() {
// return this.returnType;
// }
//
// @Override
// public Type[] getParameterTypes() {
// return this.paramTypes;
// }
//
// }
//
// class IFunction extends Interface {
// Class<?> funcType;
//
// @Override
// public Type getDeclaringClass() {
// return null;
// }
//
// @Override
// public Type getReturnType() {
// Method m = Reflector.findInvokeMethod(funcType);
// return m.getReturnType();
// }
//
// @Override
// public Type[] getParameterTypes() {
// Method m = Reflector.findInvokeMethod(funcType);
// return m.getParameterTypes();
// }
//
// }
//
// abstract class IMethod extends Interface {
// protected Method method;
//
// public IMethod(Method m) {
// this.method = m;
// }
//
// }
//
// class ISimpleMethod extends IMethod {
//
// public ISimpleMethod(Method m) {
// super(m);
// }
//
// @Override
// public Type getDeclaringClass() {
// return method.getDeclaringClass();
// }
//
// @Override
// public Type getReturnType() {
// return method.getReturnType();
// }
//
// @Override
// public Type[] getParameterTypes() {
// return method.getParameterTypes();
// }
// }
//
// class IGenericMethod extends IMethod {
//
// public IGenericMethod(Method m) {
// super(m);
// }
//
// @Override
// public Type getDeclaringClass() {
// return method.getDeclaringClass();
// }
//
// @Override
// public Type getReturnType() {
// return method.getGenericReturnType();
// }
//
// @Override
// public Type[] getParameterTypes() {
// return method.getGenericParameterTypes();
// }
// }
