package nez.lang;

import java.util.TreeMap;

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
	
	public final String getFlagName() {
		return this.flagName;
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