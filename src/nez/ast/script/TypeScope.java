package nez.ast.script;

import java.lang.reflect.Type;
import java.util.HashMap;

public class TypeScope {
	TypeScope parent;
	private HashMap<String, Type> variableTypes;

	public TypeScope() {
		this.variableTypes = new HashMap<String, Type>();
	}

	public TypeScope(TypeScope parent) {
		this();
		this.parent = parent;
	}

	public void setVarType(String name, Type type) {
		this.variableTypes.put(name, type);
	}

	public Type getVarType(String name) {
		return this.variableTypes.get(name);
	}

	public boolean containsVariable(String name) {
		return this.variableTypes.containsKey(name);
	}
}
