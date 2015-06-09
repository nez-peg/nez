package nez.lang;

import nez.ast.SourcePosition;
import nez.vm.Instruction;
import nez.vm.NezEncoder;

public class IfFlag extends Unconsumed implements Conditional {
	boolean predicate;
	String flagName;
	IfFlag(SourcePosition s, boolean predicate, String flagName) {
		super(s);
		if(flagName.startsWith("!")) {
			predicate = false;
			flagName = flagName.substring(1);
		}
		this.predicate = predicate;
		this.flagName = flagName;
	}
	@Override
	public final boolean equalsExpression(Expression o) {
		if(o instanceof IfFlag) {
			IfFlag e = (IfFlag)o;
			return this.predicate == e.predicate && this.flagName.equals(e.flagName);
		}
		return false;
	}


	public final String getFlagName() {
		return this.flagName;
	}

	public boolean isPredicate() {
		return predicate;
	}

	@Override
	public String getPredicate() {
		return predicate ? "if " + this.flagName : "if !" + this.flagName;
	}

	@Override
	public Expression reshape(GrammarReshaper m) {
		return m.reshapeIfFlag(this);
	}

	@Override
	public boolean isConsumed() {
		return false;
	}

	@Override
	public Instruction encode(NezEncoder bc, Instruction next, Instruction failjump) {
		return bc.encodeIfFlag(this, next, failjump);
	}

	@Override
	protected int pattern(GEP gep) {
		return 0;
	}

	@Override
	protected void examplfy(GEP gep, StringBuilder sb, int p) {
	}

}