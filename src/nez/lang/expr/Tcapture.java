package nez.lang.expr;

import nez.ast.SourcePosition;
import nez.lang.Expression;
import nez.lang.ExpressionVisitor;
import nez.lang.PossibleAcceptance;
import nez.lang.Typestate;
import nez.lang.Visa;
import nez.parser.AbstractGenerator;
import nez.parser.moz.MozInst;

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
	public Object visit(ExpressionVisitor v, Object a) {
		return v.visitTcapture(this, a);
	}

	@Override
	public boolean isConsumed() {
		return false;
	}

	@Override
	public int inferTypestate(Visa v) {
		return Typestate.OperationType;
	}

	@Override
	public short acceptByte(int ch) {
		return PossibleAcceptance.Unconsumed;
	}

	@Override
	public MozInst encode(AbstractGenerator bc, MozInst next, MozInst failjump) {
		return bc.encodeTcapture(this, next);
	}
}
