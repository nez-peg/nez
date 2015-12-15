package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.lang.Expression;
import nez.lang.PossibleAcceptance;

public class Tnew extends Term {
	// public boolean leftFold;
	// Symbol label = null;
	// public Expression outer = null;
	public int shift = 0;

	Tnew(SourceLocation s, int shift) {
		super(s);
		this.shift = shift;
	}

	@Override
	public final boolean equalsExpression(Expression o) {
		if (o instanceof Tnew) {
			Tnew s = (Tnew) o;
			return (this.shift == s.shift);
		}
		return false;
	}

	@Override
	public final void format(StringBuilder sb) {
		sb.append("{");
	}

	@Override
	public Object visit(Expression.Visitor v, Object a) {
		return v.visitTnew(this, a);
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
