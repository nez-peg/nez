package nez.lang;

import nez.ast.SourcePosition;
import nez.runtime.Instruction;
import nez.runtime.NezCompiler;
import nez.util.UList;
import nez.util.UMap;

public class Match extends Unary {
	Match(SourcePosition s, Expression inner) {
		super(s, inner);
	}
	
	@Override
	public String getPredicate() { 
		return "~";
	}
	
	@Override
	public String key() { 
		return "~";
	}
	
	@Override
	public Expression reshape(Manipulator m) {
		return m.reshapeMatch(this);
	}

	@Override
	public boolean isConsumed(Stacker stacker) {
		return this.inner.isConsumed(stacker);
	}

	@Override
	public boolean checkAlwaysConsumed(GrammarChecker checker, String startNonTerminal, UList<String> stack) {
		return this.inner.checkAlwaysConsumed(checker, startNonTerminal, stack);
	}
	@Override
	public int inferTypestate(UMap<String> visited) {
		return Typestate.BooleanType;
	}
	@Override
	public short acceptByte(int ch, int option) {
		return this.inner.acceptByte(ch, option);
	}
	@Override
	public Instruction encode(NezCompiler bc, Instruction next) {
		return this.inner.encode(bc, next);
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