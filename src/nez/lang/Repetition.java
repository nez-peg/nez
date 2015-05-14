package nez.lang;

import nez.ast.SourcePosition;
import nez.runtime.Instruction;
import nez.runtime.RuntimeCompiler;
import nez.util.UList;
import nez.util.UMap;

public class Repetition extends Unary {
	public boolean possibleInfiniteLoop = false;
	Repetition(SourcePosition s, Expression e) {
		super(s, e);
	}
	@Override
	public String getPredicate() { 
		return "*";
	}
	@Override
	public String getInterningKey() { 
		return "*";
	}
	@Override
	public Expression reshape(Manipulator m) {
		return m.reshapeRepetition(this);
	}
	@Override
	public boolean checkAlwaysConsumed(GrammarChecker checker, String startNonTerminal, UList<String> stack) {
//		if(checker != null) {
//			this.inner.checkAlwaysConsumed(checker, startNonTerminal, stack);
//		}
		return false;
	}
	@Override void checkPhase1(GrammarChecker checker, String ruleName, UMap<String> visited, int depth) {
		this.inner.setOuterLefted(this);
	}
	@Override void checkPhase2(GrammarChecker checker) {
		if(!this.inner.checkAlwaysConsumed(checker, null, null)) {
			checker.reportError(s, "unconsumed repetition");
			this.possibleInfiniteLoop = true;
		}
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
	public Instruction encode(RuntimeCompiler bc, Instruction next) {
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