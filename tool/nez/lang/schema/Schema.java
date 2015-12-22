package nez.lang.schema;

import nez.lang.Expression;

public class Schema {
	private Expression schemaExpression;
	private Schema next;

	public Schema(Expression e, Schema t) {
		this.schemaExpression = e;
		this.next = t;
	}

	public Schema(Expression e) {
		this.schemaExpression = e;
	}

	public Expression getSchemaExpression() {
		return this.schemaExpression;
	}

	public void setSchemaExpression(Expression e) {
		this.schemaExpression = e;
	}

	public Schema next() {
		return this.next;
	}
}
