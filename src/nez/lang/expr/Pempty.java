package nez.lang.expr;

import nez.ast.SourcePosition;
import nez.lang.Expression;
import nez.lang.GrammarTransducer;
import nez.lang.PossibleAcceptance;
import nez.lang.Typestate;
import nez.lang.Visa;
import nez.parser.Instruction;
import nez.parser.AbstractGenerator;

public class Pempty extends Term {
	Pempty(SourcePosition s) {
		super(s);
	}

	@Override
	public final boolean equalsExpression(Expression o) {
		return (o instanceof Pempty);
	}

	@Override
	public final void format(StringBuilder sb) {
		sb.append("''");
	}

	@Override
	public Expression reshape(GrammarTransducer m) {
		return m.reshapePempty(this);
	}

	@Override
	public boolean isConsumed() {
		return false;
	}

	@Override
	public int inferTypestate(Visa v) {
		return Typestate.BooleanType;
	}

	@Override
	public short acceptByte(int ch) {
		return PossibleAcceptance.Unconsumed;
	}

	@Override
	public Instruction encode(AbstractGenerator bc, Instruction next, Instruction failjump) {
		return bc.encodeTempty(this, next);
	}
}
