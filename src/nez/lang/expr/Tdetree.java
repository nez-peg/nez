package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.lang.Expression;

public class Tdetree extends Unary {
	Tdetree(SourceLocation s, Expression inner) {
		super(s, inner);
	}

	@Override
	public final boolean equalsExpression(Expression o) {
		if (o instanceof Tdetree) {
			return this.get(0).equalsExpression(o.get(0));
		}
		return false;
	}

	@Override
	public final void format(StringBuilder sb) {
		this.formatUnary(sb, "~", inner, null);
	}

	@Override
	public Object visit(Expression.Visitor v, Object a) {
		return v.visitTdetree(this, a);
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