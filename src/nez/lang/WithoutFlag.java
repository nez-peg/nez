package nez.lang;

import nez.ast.SourcePosition;
import nez.util.UList;
import nez.util.UMap;
import nez.vm.Instruction;
import nez.vm.NezCompiler;

@Deprecated
public class WithoutFlag extends Unary {
	String flagName;
	WithoutFlag(SourcePosition s, String flagName, Expression inner) {
		super(s, inner);
		this.flagName = flagName;
		this.optimized = inner.optimized;
	}
	@Override
	public String getPredicate() {
		return "without " + this.flagName;
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
	@Override
	public Expression reshape(GrammarReshaper m) {
		// TODO Auto-generated method stub
		return null;
	}

}