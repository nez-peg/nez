package nez.lang;

import nez.ast.SourcePosition;
import nez.util.UList;
import nez.util.UMap;
import nez.vm.Instruction;
import nez.vm.NezCompiler;

public class Option extends Unary {
	Option(SourcePosition s, Expression e) {
		super(s, e);
		e.setOuterLefted(this);
	}
	@Override
	public String getPredicate() { 
		return "?";
	}
	@Override
	public String key() { 
		return "?";
	}
	@Override
	public Expression reshape(GrammarReshaper m) {
		return m.reshapeOption(this);
	}

	@Override
	public boolean isConsumed(Stacker stacker) {
		return false;
	}

	@Override
	public boolean checkAlwaysConsumed(GrammarChecker checker, String startNonTerminal, UList<String> stack) {
		return false;
	}

	@Override
	public int inferTypestate(UMap<String> visited) {
		int t = this.inner.inferTypestate(visited);
		if(t == Typestate.ObjectType) {
			return Typestate.BooleanType;
		}
		return t;
	}
	
	@Override public short acceptByte(int ch, int option) {
		return Prediction.acceptOption(this, ch, option);
	}
	
	@Override
	public Instruction encode(NezCompiler bc, Instruction next, Instruction failjump) {
		return bc.encodeOption(this, next);
	}

	@Override
	protected int pattern(GEP gep) {
		return 2;
	}
	@Override
	protected void examplfy(GEP gep, StringBuilder sb, int p) {
		if(p % 2 == 0) {
			this.inner.examplfy(gep, sb, p);
		}
	}

}