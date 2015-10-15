package konoha.script;

import java.lang.reflect.Type;
import java.util.HashMap;

public class TypeScope {
	TypeScope parent;
	private HashMap<String, Type> variableTypes = null;

	public TypeScope() {
	}

	public TypeScope(TypeScope parent) {
		this();
		this.parent = parent;
	}

	public void setVarType(String name, Type type) {
		if (this.variableTypes == null) {
			this.variableTypes = new HashMap<>();
		}
		this.variableTypes.put(name, type);
	}

	public Type getVarType(String name) {
		TypeScope scope = this;
		while (scope != null) {
			if (scope.variableTypes != null && scope.variableTypes.containsKey(name)) {
				return scope.variableTypes.get(name);
			}
			scope = scope.parent;
		}
		return null;
	}

	public boolean containsVariable(String name) {
		TypeScope scope = this;
		while (scope != null) {
			if (scope.variableTypes != null) {
				if (scope.variableTypes.containsKey(name)) {
					return true;
				}
			}
			scope = scope.parent;
		}
		return false;
	}
}
