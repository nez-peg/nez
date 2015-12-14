package nez.lang.expr;

import nez.ast.SourcePosition;
import nez.lang.Expression;

public class Xblock extends Unary {
	Xblock(SourcePosition s, Expression e) {
		super(s, e);
	}

	@Override
	public final boolean equalsExpression(Expression o) {
		if (o instanceof Xblock) {
			return this.get(0).equalsExpression(o.get(0));
		}
		return false;
	}

	@Override
	public Object visit(Expression.Visitor v, Object a) {
		return v.visitXblock(this, a);
	}

	@Override
	public boolean isConsumed() {
		return this.inner.isConsumed();
	}

	@Override
	public short acceptByte(int ch) {
		return this.inner.acceptByte(ch);
	}

}