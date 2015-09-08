package nez.lang.expr;

import nez.ast.SourcePosition;
import nez.lang.Expression;
import nez.lang.GrammarReshaper;
import nez.lang.PossibleAcceptance;
import nez.lang.Typestate;
import nez.lang.Visa;
import nez.vm.Instruction;
import nez.vm.NezEncoder;

public class Repetition extends Unary {
	public boolean possibleInfiniteLoop = false;

	Repetition(SourcePosition s, Expression e) {
		super(s, e);
		// e.setOuterLefted(this);
	}

	@Override
	public boolean equalsExpression(Expression o) {
		if (o instanceof Repetition) {
			return this.get(0).equalsExpression(o.get(0));
		}
		return false;
	}

	@Override
	public void format(StringBuilder sb) {
		this.formatUnary(sb, this.inner, "*");
	}

	@Override
	public Expression reshape(GrammarReshaper m) {
		return m.reshapeRepetition(this);
	}

	@Override
	public boolean isConsumed() {
		return false;
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
		return PossibleAcceptance.acceptOption(this, ch);
	}

	@Override
	public Instruction encode(NezEncoder bc, Instruction next, Instruction failjump) {
		return bc.encodeRepetition(this, next);
	}

}