package nez.lang;

import nez.ast.SourcePosition;
import nez.runtime.Instruction;
import nez.runtime.RuntimeCompiler;
import nez.util.UList;
import nez.util.UMap;

public class Link extends Unary {
	public int index;
	Link(SourcePosition s, Expression e, int index) {
		super(s, e);
		this.index = index;
	}
	@Override
	public String getPredicate() { 
		return (index != -1) ? "link " + index : "link";
	}
	@Override
	public String key() {
		return (index != -1) ? "@" + index : "@";
	}
	@Override
	public Expression reshape(Manipulator m) {
		return m.reshapeLink(this);
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
		return Typestate.OperationType;
	}
	@Override
	public short acceptByte(int ch, int option) {
		return inner.acceptByte(ch, option);
	}
	@Override
	public Instruction encode(RuntimeCompiler bc, Instruction next) {
		return bc.encodeLink(this, next);
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