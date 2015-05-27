package nez.lang;

import nez.ast.SourcePosition;
import nez.util.UList;
import nez.util.UMap;
import nez.vm.Instruction;
import nez.vm.NezCompiler;

public class Repetition extends Unary {
	public boolean possibleInfiniteLoop = false;
	Repetition(SourcePosition s, Expression e) {
		super(s, e);
		e.setOuterLefted(this);
	}
	@Override
	public String getPredicate() { 
		return "*";
	}
	@Override
	public String key() { 
		return "*";
	}
	@Override
	public Expression reshape(GrammarReshaper m) {
		return m.reshapeRepetition(this);
	}

	@Override
	public boolean isConsumed(Stacker stacker) {
		return false;
	}

	@Override
	public boolean checkAlwaysConsumed(GrammarChecker checker, String startNonTerminal, UList<String> stack) {
//		if(checker != null) {
//			this.inner.checkAlwaysConsumed(checker, startNonTerminal, stack);
//		}
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
		return bc.encodeRepetition(this, next);
	}

	@Override
	protected int pattern(GEP gep) {
		return 2;
	}
	@Override
	protected void examplfy(GEP gep, StringBuilder sb, int p) {
		if(p > 0) {
			int p2 = this.inner.pattern(gep);
			for(int i = 0; i < p2; i++) {
				this.inner.examplfy(gep, sb, p2);
			}
		}
	}
}