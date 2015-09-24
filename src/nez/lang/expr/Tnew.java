package nez.lang.expr;

import nez.ast.SourcePosition;
import nez.lang.Expression;
import nez.lang.GrammarTransducer;
import nez.lang.PossibleAcceptance;
import nez.lang.Typestate;
import nez.lang.Visa;
import nez.parser.AbstractGenerator;
import nez.parser.Instruction;

public class Tnew extends Term {
	// public boolean leftFold;
	// Symbol label = null;
	// public Expression outer = null;
	public int shift = 0;

	Tnew(SourcePosition s, int shift) {
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
	public Expression reshape(GrammarTransducer m) {
		return m.reshapeTnew(this);
	}

	@Override
	public boolean isConsumed() {
		return false;
	}

	@Override
	public short acceptByte(int ch) {
		return PossibleAcceptance.Unconsumed;
	}

	@Override
	public int inferTypestate(Visa v) {
		return Typestate.ObjectType;
	}

	@Override
	public Instruction encode(AbstractGenerator bc, Instruction next, Instruction failjump) {
		return bc.encodeTnew(this, next);
	}
}
