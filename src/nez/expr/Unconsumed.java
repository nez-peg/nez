package nez.expr;

import java.util.TreeMap;

import nez.ast.SourcePosition;
import nez.util.UList;
import nez.util.UMap;

abstract class Unconsumed extends Expression {
	protected Unconsumed(SourcePosition s) {
		super(s);
	}
	@Override
	public final int size() {
		return 0;
	}
	@Override
	public final Expression get(int index) {
		return null;
	}
	@Override
	public String getInterningKey() {
		return this.getPredicate();
	}
	@Override
	public boolean checkAlwaysConsumed(GrammarChecker checker, String startNonTerminal, UList<String> stack) {
		return false;
	}
	@Override
	public int inferTypestate(UMap<String> visited) {
		return Typestate.BooleanType;
	}
	@Override
	public Expression checkTypestate(GrammarChecker checker, Typestate c) {
		return this;
	}
	@Override
	public Expression removeASTOperator(boolean newNonTerminal) {
		return this;
	}
	@Override
	public Expression removeFlag(TreeMap<String, String> undefedFlags) {
		return this;
	}
	@Override
	public short acceptByte(int ch, int option) {
		return Prediction.Unconsumed;
	}
}
