package nez.lang;

import java.util.TreeMap;

import nez.ast.SourcePosition;
import nez.runtime.RuntimeCompiler;
import nez.runtime.Instruction;

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
	public Expression reshape(Manipulator m) {
		return m.reshapeIfFlag(this);
	}
	@Override
	public Instruction encode(RuntimeCompiler bc, Instruction next) {
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