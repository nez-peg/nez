package nez.ast.jcode;

import org.objectweb.asm.Type;

public class StaticField {
	Class<?> ownerClass;
	String name;
	Class<?> fieldClass;
	Type ownerType;
	Type fieldType;

	public StaticField(Class<?> owner, String name, Class<?> field) {
		this.ownerClass = owner;
		this.ownerType = Type.getType(owner);
		this.name = name;
		this.fieldClass = field;
		this.fieldType = Type.getType(field);
	}

	public Type getOwnerType() {
		return this.ownerType;
	}

	public String getFieldName() {
		return this.name;
	}

	public Type getFieldType() {
		return this.fieldType;
	}
}
