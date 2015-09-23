package nez.lang.schema;

import nez.lang.Expression;

public class Type {
	private Expression typeExpression;
	private String elementName;
	private Type next;

	public Type(String name, Expression e, Type t) {
		this.elementName = name;
		this.typeExpression = e;
		this.next = t;
	}

	public Type(Expression e) {
		this.typeExpression = e;
	}

	public String getElementName() {
		return this.elementName;
	}

	public Expression getTypeExpression() {
		return this.typeExpression;
	}

	public Type next() {
		return this.next;
	}
}
