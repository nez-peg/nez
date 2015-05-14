package nez.lang;

import nez.ast.SourcePosition;
import nez.runtime.Instruction;
import nez.runtime.RuntimeCompiler;
import nez.util.UList;
import nez.util.UMap;

public class Repetition1 extends Repetition {
	Repetition1(SourcePosition s, Expression e) {
		super(s, e);
		e.setOuterLefted(this);
	}
	@Override
	public String getPredicate() { 
		return "+";
	}
	@Override
	public String key() { 
		return "+";
	}
	@Override
	public Expression reshape(Manipulator m) {
		return m.reshapeRepetition1(this);
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
		int t = this.inner.inferTypestate(visited);
		if(t == Typestate.ObjectType) {
			return Typestate.BooleanType;
		}
		return t;
	}

	@Override public short acceptByte(int ch, int option) {
		return Prediction.acceptUnary(this, ch, option);
	}
	
	@Override
	public Instruction encode(RuntimeCompiler bc, Instruction next) {
		return bc.encodeRepetition1(this, next);
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
