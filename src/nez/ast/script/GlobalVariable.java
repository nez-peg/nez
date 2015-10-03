package nez.ast.script;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

public class GlobalVariable {
	Type type;
	Class<?> varClass;
	Field field;

	GlobalVariable(Type type, Class<?> varClass) {
		this.type = type;
		this.varClass = varClass;
		try {
			this.field = varClass.getField("v");
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
	}

	public Type getType() {
		return this.type;
	}

	public Field getField() {
		return this.field;
	}

}
