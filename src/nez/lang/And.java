package nez.lang;

import nez.ast.SourcePosition;
import nez.util.UList;
import nez.util.UMap;
import nez.vm.Instruction;
import nez.vm.NezCompiler;

public class And extends Unary {
	And(SourcePosition s, Expression e) {
		super(s, e);
	}
	@Override
	public String getPredicate() {
		return "&";
	}

	@Override
	public Expression reshape(GrammarReshaper m) {
		return m.reshapeAnd(this);
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
		if(t == Typestate.ObjectType) {  // typeCheck needs to report error
			return Typestate.BooleanType;
		}
		return t;
	}

	@Override
	public short acceptByte(int ch, int option) {
		short r = this.inner.acceptByte(ch, option);
		if(r == Prediction.Accept || r == Prediction.Unconsumed) {
			return Prediction.Unconsumed;
		}
		return r;
	}
	
	@Override
	public Instruction encode(NezCompiler bc, Instruction next, Instruction failjump) {
		return bc.encodeAnd(this, next, failjump);
	}
	
	@Override
	protected int pattern(GEP gep) {
		return -1;
	}
	@Override
	protected void examplfy(GEP gep, StringBuilder sb, int p) {
		// TODO Auto-generated method stub
	}
	
}