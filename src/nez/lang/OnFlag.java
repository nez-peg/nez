package nez.lang;

import nez.ast.SourcePosition;
import nez.util.UList;
import nez.util.UMap;
import nez.vm.Instruction;
import nez.vm.NezCompiler;

public class OnFlag extends Unary {
	boolean predicate;
	String flagName;

	OnFlag(SourcePosition s, boolean predicate, String flagName, Expression inner) {
		super(s, inner);
		if(flagName.startsWith("!")) {
			predicate = false;
			flagName = flagName.substring(1);
		}
		this.predicate = predicate;
		this.flagName = flagName;
		this.optimized = inner.optimized;
	}

	public final String getFlagName() {
		return this.flagName;
	}

	public boolean isPredicate() {
		return predicate;
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
	public boolean isConsumed(Stacker stacker) {
		return this.inner.isConsumed(stacker);
	}

	@Override
	public boolean checkAlwaysConsumed(GrammarChecker checker, String startNonTerminal, UList<String> stack) {
		return inner.checkAlwaysConsumed(checker, startNonTerminal, stack);
	}

	@Override
	public int inferTypestate(UMap<String> visited) {
		return this.inner.inferTypestate(visited);
	}

	@Override
	public short acceptByte(int ch, int option) {
		return this.inner.acceptByte(ch, option);
	}

	@Override
	public Instruction encode(NezCompiler bc, Instruction next, Instruction failjump) {
		return this.inner.encode(bc, next, failjump);
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