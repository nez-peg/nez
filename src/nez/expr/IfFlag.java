package nez.expr;

import java.util.TreeMap;

import nez.ast.SourcePosition;
import nez.runtime.RuntimeCompiler;
import nez.runtime.Instruction;

public class IfFlag extends Unconsumed {
	String flagName;
	IfFlag(SourcePosition s, String flagName) {
		super(s);
		this.flagName = flagName;
	}
	@Override
	public String getPredicate() {
		return "if " + this.flagName;
	}
	@Override
	public Expression removeFlag(TreeMap<String,String> undefedFlags) {
		if(undefedFlags != null && undefedFlags.containsKey(flagName)) {
			return Factory.newFailure(this.s);
		}
		return this;
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