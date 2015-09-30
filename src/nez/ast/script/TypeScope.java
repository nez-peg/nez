package nez.ast.script;

import java.util.HashMap;

public class TypeScope {
	TypeScope parent;
	private HashMap<String, Class<?>> variableTypes;

	public TypeScope() {
		this.variableTypes = new HashMap<String, Class<?>>();
	}

	public TypeScope(TypeScope parent) {
		this();
		this.parent = parent;
	}

	public void setVarType(String name, Class<?> type) {
		this.variableTypes.put(name, type);
	}

	public Class<?> getVarType(String name) {
		return this.variableTypes.get(name);
	}

	public boolean containsVariable(String name) {
		return this.variableTypes.containsKey(name);
	}
}
