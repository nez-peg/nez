package nez.lang.schema;

import nez.lang.Expression;

public class Type {
	private Expression typeExpression;

	public Type(Expression t) {
		this.typeExpression = t;
	}

	public Expression getTypeExpression() {
		return typeExpression;
	}
}
