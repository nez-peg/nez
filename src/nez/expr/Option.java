package nez.expr;

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
		return "*";
	}
	@Override
	public String getInterningKey() { 
		return "?";
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
	@Override
	public Expression checkTypestate(GrammarChecker checker, Typestate c) {
		int required = c.required;
		Expression inn = this.inner.checkTypestate(checker, c);
		if(required != Typestate.OperationType && c.required == Typestate.OperationType) {
			checker.reportWarning(s, "unable to create objects in repetition => removed!!");
			this.inner = inn.removeASTOperator(Expression.CreateNonTerminal);
			c.required = required;
		}
		else {
			this.inner = inn;
		}
		return this;
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