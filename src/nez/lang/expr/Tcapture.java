package nez.lang.expr;

import nez.ast.SourcePosition;
import nez.lang.Expression;
import nez.lang.ExpressionTransducer;
import nez.lang.PossibleAcceptance;
import nez.lang.Typestate;
import nez.lang.Visa;
import nez.vm.Instruction;
import nez.vm.NezEncoder;

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
	public Expression reshape(ExpressionTransducer m) {
		return m.reshapeCapture(this);
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
	public Instruction encode(NezEncoder bc, Instruction next, Instruction failjump) {
		return bc.encodeTcapture(this, next);
	}
}
