package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.lang.Expression;
import nez.lang.PossibleAcceptance;

public class Poption extends Unary {
	Poption(SourceLocation s, Expression e) {
		super(s, e);
		// e.setOuterLefted(this);
	}

	@Override
	public final boolean equalsExpression(Expression o) {
		if (o instanceof Poption) {
			return this.get(0).equalsExpression(o.get(0));
		}
		return false;
	}

	@Override
	public final void format(StringBuilder sb) {
		this.formatUnary(sb, null, this.inner, "?");
	}

	@Override
	public Object visit(Expression.Visitor v, Object a) {
		return v.visitPoption(this, a);
	}

	@Override
	public boolean isConsumed() {
		return false;
	}

	@Override
	public short acceptByte(int ch) {
		return PossibleAcceptance.acceptOption(this, ch);
	}

}