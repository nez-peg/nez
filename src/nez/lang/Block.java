package nez.lang;

import nez.ast.SourcePosition;
import nez.runtime.Instruction;
import nez.runtime.NezCompiler;
import nez.util.UList;
import nez.util.UMap;

public class Block extends Unary {
	Block(SourcePosition s, Expression e) {
		super(s, e);
	}
	@Override
	public String getPredicate() {
		return "block";
	}
	@Override
	public Expression reshape(Manipulator m) {
		return m.reshapeBlock(this);
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
		return this.inner.inferTypestate(visited);
	}

	@Override
	public short acceptByte(int ch, int option) {
		return this.inner.acceptByte(ch, option);
	}
	
	@Override
	public Instruction encode(NezCompiler bc, Instruction next) {
		return bc.encodeBlock(this, next);
	}
	
	@Override
	protected int pattern(GEP gep) {
		return inner.pattern(gep);
	}

	@Override
	protected void examplfy(GEP gep, StringBuilder sb, int p) {
		int stacktop = gep.beginBlock();
		this.inner.examplfy(gep, sb, p);
		gep.endBlock(stacktop);
	}

}