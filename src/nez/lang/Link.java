package nez.lang;

import nez.ast.SourcePosition;
import nez.util.UList;
import nez.util.UMap;
import nez.vm.Instruction;
import nez.vm.NezEncoder;

public class Link extends Unary {
	public int index;
	Link(SourcePosition s, Expression e, int index) {
		super(s, e);
		this.index = index;
	}
	@Override
	public final boolean equalsExpression(Expression o) {
		if(o instanceof Link && this.index == ((Link)o).index) {
			return this.get(0).equalsExpression(o.get(0));
		}
		return false;
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
	protected final void format(StringBuilder sb) {
		formatUnary(sb, (index != -1) ? "@[" + index +"]" : "@", this.get(0));
	}
	@Override
	public Expression reshape(GrammarReshaper m) {
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
	public Instruction encode(NezEncoder bc, Instruction next, Instruction failjump) {
		return bc.encodeLink(this, next, failjump);
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