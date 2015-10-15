package konoha.script;

import java.lang.reflect.Type;

public class FunctionBuilder {
	FunctionBuilder parent;
	String name;
	TypeScope scope;
	Type returnType = null;
	private Type[] paramTypes;

	FunctionBuilder(FunctionBuilder parent, String name) {
		this.parent = parent;
		this.name = name;
		this.scope = new TypeScope();
	}

	public final FunctionBuilder pop() {
		return this.parent;
	}

	public void beginLocalVarScope() {
		this.scope = new TypeScope(this.scope);
	}

	public void endLocalVarScope() {
		this.scope = this.scope.parent;
	}

	public void setVarType(String name, Type type) {
		this.scope.setVarType(name, type);
	}

	public Type getVarType(String name) {
		return this.scope.getVarType(name);
	}

	public boolean containsVariable(String name) {
		return this.scope.containsVariable(name);
	}

	public void setReturnType(Type returnType) {
		this.returnType = returnType;
	}

	public Type getReturnType() {
		return this.returnType;
	}

	public final String getName() {
		return this.name;
	}

	public void setParameterTypes(Type[] paramTypes) {
		this.paramTypes = paramTypes;
	}

	public Type[] getParameterTypes() {
		return this.paramTypes;
	}
}
