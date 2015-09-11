package nez.lang.expr;

import nez.ast.SourcePosition;
import nez.lang.Expression;
import nez.lang.GrammarTransducer;
import nez.lang.PossibleAcceptance;
import nez.lang.Typestate;
import nez.lang.Visa;
import nez.vm.Instruction;
import nez.vm.NezEncoder;

public class Pnot extends Unary {
	Pnot(SourcePosition s, Expression e) {
		super(s, e);
	}

	@Override
	public final boolean equalsExpression(Expression o) {
		if (o instanceof Pnot) {
			return this.get(0).equalsExpression(o.get(0));
		}
		return false;
	}

	@Override
	public final void format(StringBuilder sb) {
		this.formatUnary(sb, "!", this.inner);
	}

	@Override
	public Expression reshape(GrammarTransducer m) {
		return m.reshapePnot(this);
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
		return PossibleAcceptance.acceptNot(this, ch);
	}

	@Override
	public Instruction encode(NezEncoder bc, Instruction next, Instruction failjump) {
		return bc.encodePnot(this, next, failjump);
	}

}