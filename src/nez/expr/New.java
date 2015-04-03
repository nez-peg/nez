package nez.expr;

import java.util.TreeMap;

import nez.ast.SourcePosition;
import nez.runtime.Instruction;
import nez.runtime.RuntimeCompiler;
import nez.util.UList;
import nez.util.UMap;

public class New extends Unconsumed {
	public boolean lefted;
	public Expression outer = null;
	public int shift  = 0;
	New(SourcePosition s, boolean lefted, int shift) {
		super(s);
		this.lefted = lefted;
		this.shift  = shift;
	}
	@Override
	public String getPredicate() { 
		return "new";
	}
	@Override
	public String getInterningKey() {
		String s = lefted ? "{@" : "{";
		return (shift != 0) ? s + "[" + shift + "]" : s;
	}
	@Override
	public boolean checkAlwaysConsumed(GrammarChecker checker, String startNonTerminal, UList<String> stack) {
		return false;
	}
	
	@Override 
	boolean setOuterLefted(Expression outer) { 
		if(this.lefted) {
			this.outer = outer;
			return false;
		}
		return false; 
	}
	@Override void checkPhase2(GrammarChecker checker) {
		if(this.lefted && this.outer == null) {
			checker.reportError(s, "expected repetition for " + this);
		}
	}

	@Override
	public Expression removeASTOperator(boolean newNonTerminal) {
		return Factory.newEmpty(s);
	}
	@Override
	public int inferTypestate(UMap<String> visited) {
		return Typestate.ObjectType;
	}
	@Override
	public Expression checkTypestate(GrammarChecker checker, Typestate c) {
		if(this.lefted) {
			if(c.required != Typestate.OperationType) {
				checker.reportWarning(s, "unexpected {@ .. => removed!!");
				return this.removeASTOperator(Expression.CreateNonTerminal);
			}
		}
		else {
			if(c.required != Typestate.ObjectType) {
				checker.reportWarning(s, "unexpected { .. => removed!");
				return Factory.newEmpty(s);
			}
		}
		c.required = Typestate.OperationType;
		return this;
	}
	@Override
	public Expression removeFlag(TreeMap<String, String> undefedFlags) {
		return this;
	}
	@Override
	public Instruction encode(RuntimeCompiler bc, Instruction next) {
		return bc.encodeNew(this, next);
	}
	@Override
	protected int pattern(GEP gep) {
		int max = 0;
		for(Expression p: this) {
			int c = p.pattern(gep);
			if(c > max) {
				max = c;
			}
		}
		return max;
	}
	@Override
	protected void examplfy(GEP gep, StringBuilder sb, int p) {
		for(Expression e: this) {
			e.examplfy(gep, sb, p);
		}
	}
}
