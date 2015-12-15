package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.lang.Expression;
import nez.lang.PossibleAcceptance;

public class Pfail extends Term {
	Pfail(SourceLocation s) {
		super(s);
	}

	@Override
	public final boolean equalsExpression(Expression o) {
		return (o instanceof Pfail);
	}

	@Override
	public final void format(StringBuilder sb) {
		sb.append("!''");
	}

	@Override
	public Object visit(Expression.Visitor v, Object a) {
		return v.visitPfail(this, a);
	}

	@Override
	public boolean isConsumed() {
		return true;
	}

	@Override
	public short acceptByte(int ch) {
		return PossibleAcceptance.Reject;
	}

}