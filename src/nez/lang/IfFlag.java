package nez.lang;

import nez.ast.SourcePosition;
import nez.vm.Instruction;
import nez.vm.NezCompiler;

public class IfFlag extends Unconsumed {
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
	public boolean isConsumed(Stacker stacker) {
		return false;
	}

	@Override
	public Instruction encode(NezCompiler bc, Instruction next, Instruction failjump) {
		return next;
	}

	@Override
	protected int pattern(GEP gep) {
		return 0;
	}

	@Override
	protected void examplfy(GEP gep, StringBuilder sb, int p) {
	}

}