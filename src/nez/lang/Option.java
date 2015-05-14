package nez.lang;

import nez.ast.SourcePosition;
import nez.runtime.Instruction;
import nez.runtime.RuntimeCompiler;
import nez.util.UList;
import nez.util.UMap;

public class Option extends Unary {
	Option(SourcePosition s, Expression e) {
		super(s, e);
	}
	@Override
	Expression dupUnary(Expression e) {
		return (this.inner != e) ? Factory.newOption(this.s, e) : this;
	}
	@Override
	public String getPredicate() { 
		return "?";
	}
	@Override
	public String getInterningKey() { 
		return "?";
	}
	@Override
	public Expression reshape(Manipulator m) {
		return m.reshapeOption(this);
	}
	@Override
	public boolean checkAlwaysConsumed(GrammarChecker checker, String startNonTerminal, UList<String> stack) {
		return false;
	}
	@Override void checkPhase1(GrammarChecker checker, String ruleName, UMap<String> visited, int depth) {
		this.inner.setOuterLefted(this);
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