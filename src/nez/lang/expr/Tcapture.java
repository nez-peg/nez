package nez.lang.expr;

import nez.ast.SourcePosition;
import nez.lang.Expression;
import nez.lang.PossibleAcceptance;

public class Tcapture extends Term {
	public int shift;

	Tcapture(SourcePosition s, int shift) {
		super(s);
		this.shift = shift;
	}

	@Override
	public final boolean equalsExpression(Expression o) {
		return (o instanceof Tcapture && this.shift == ((Tcapture) o).shift);
	}

	@Override
	public final void format(StringBuilder sb) {
		sb.append("}");
	}

	@Override
	public Object visit(Expression.Visitor v, Object a) {
		return v.visitTcapture(this, a);
	}

	@Override
	public boolean isConsumed() {
		return false;
	}

	@Override
	public short acceptByte(int ch) {
		return PossibleAcceptance.Unconsumed;
	}

}
