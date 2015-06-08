package nez.lang;

import nez.ast.SourcePosition;
import nez.vm.Instruction;
import nez.vm.NezEncoder;

public class OnFlag extends Unary implements Conditional {
	boolean predicate;
	public final boolean isPositive() {
		return predicate;
	}
	String flagName;
	public final String getFlagName() {
		return this.flagName;
	}

	OnFlag(SourcePosition s, boolean predicate, String flagName, Expression inner) {
		super(s, inner);
		if(flagName.startsWith("!")) {
			predicate = false;
			flagName = flagName.substring(1);
		}
		this.predicate = predicate;
		this.flagName = flagName;
	}
	@Override
	public final boolean equalsExpression(Expression o) {
		if(o instanceof OnFlag) {
			OnFlag e = (OnFlag)o;
			if(this.predicate == e.predicate && this.flagName.equals(e.flagName)) {
				return this.get(0).equalsExpression(e.get(0));
			};
		}
		return false;
	}

	@Override
	public String getPredicate() {
		return predicate ? "on " + this.flagName : "on !" + this.flagName;
	}

	@Override
	public Expression reshape(GrammarReshaper m) {
		return m.reshapeOnFlag(this);
	}

	@Override
	public boolean isConsumed() {
		return this.inner.isConsumed();
	}

	@Override
	public int inferTypestate(Visa v) {
		return this.inner.inferTypestate(v);
	}

	@Override
	public short acceptByte(int ch, int option) {
		return this.inner.acceptByte(ch, option);
	}

	@Override
	public Instruction encode(NezEncoder bc, Instruction next, Instruction failjump) {
		return bc.encodeOnFlag(this, next, failjump);
	}

	@Override
	protected int pattern(GEP gep) {
		return inner.pattern(gep);
	}

	@Override
	protected void examplfy(GEP gep, StringBuilder sb, int p) {
		this.inner.examplfy(gep, sb, p);
	}

}