package nez.lang;

import nez.ast.SourcePosition;
import nez.vm.Instruction;
import nez.vm.NezEncoder;

public class Repetition extends Unary {
	public boolean possibleInfiniteLoop = false;

	Repetition(SourcePosition s, Expression e) {
		super(s, e);
		e.setOuterLefted(this);
	}

	@Override
	public boolean equalsExpression(Expression o) {
		if(o instanceof Repetition) {
			return this.get(0).equalsExpression(o.get(0));
		}
		return false;
	}

	@Override
	public String getPredicate() {
		return "*";
	}

	@Override
	public String key() {
		return "*";
	}

	@Override
	protected void format(StringBuilder sb) {
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
		if(t == Typestate.ObjectType) {
			return Typestate.BooleanType;
		}
		return t;
	}

	@Override
	public short acceptByte(int ch) {
		return PossibleAcceptance.acceptOption(this, ch);
	}

	@Override
	public Instruction encode(NezEncoder bc, Instruction next,
			Instruction failjump) {
		return bc.encodeRepetition(this, next);
	}

	@Override
	protected int pattern(GEP gep) {
		return 2;
	}

	@Override
	protected void examplfy(GEP gep, StringBuilder sb, int p) {
		if(p > 0) {
			int p2 = this.inner.pattern(gep);
			for(int i = 0; i < p2; i++) {
				this.inner.examplfy(gep, sb, p2);
			}
		}
	}
}
