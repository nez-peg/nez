package nez.lang.expr;

import nez.ast.SourcePosition;
import nez.lang.Expression;
import nez.lang.GrammarTransducer;
import nez.lang.PossibleAcceptance;
import nez.lang.Typestate;
import nez.lang.Visa;
import nez.parser.Instruction;
import nez.parser.AbstractGenerator;

public class Pone extends Pzero {
	Pone(SourcePosition s, Expression e) {
		super(s, e);
	}

	@Override
	public final boolean equalsExpression(Expression o) {
		if (o instanceof Pone) {
			return this.get(0).equalsExpression(o.get(0));
		}
		return false;
	}

	@Override
	public final void format(StringBuilder sb) {
		this.formatUnary(sb, this.inner, "+");
	}

	@Override
	public Expression reshape(GrammarTransducer m) {
		return m.reshapePone(this);
	}

	@Override
	public boolean isConsumed() {
		return this.inner.isConsumed();
	}

	@Override
	public int inferTypestate(Visa v) {
		int t = this.inner.inferTypestate(v);
		if (t == Typestate.ObjectType) {
			return Typestate.BooleanType;
		}
		return t;
	}

	@Override
	public short acceptByte(int ch) {
		return PossibleAcceptance.acceptUnary(this, ch);
	}

	@Override
	public Instruction encode(AbstractGenerator bc, Instruction next, Instruction failjump) {
		return bc.encodePone(this, next, failjump);
	}

}
