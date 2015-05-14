package nez.lang;

import java.util.TreeMap;

import nez.ast.SourcePosition;
import nez.util.UList;
import nez.util.UMap;

public abstract class ASTOperation extends Expression {
	ASTOperation(SourcePosition s) {
		super(s);
	}

	@Override
	public Expression get(int index) {
		return null;
	}

	@Override
	public int size() {
		return 0;
	}

	@Override
	public boolean checkAlwaysConsumed(GrammarChecker checker, String startNonTerminal, UList<String> stack) {
		return false;
	}
	@Override
	public int inferTypestate(UMap<String> visited) {
		return Typestate.OperationType;
	}
	@Override
	public short acceptByte(int ch, int option) {
		return Prediction.Unconsumed;
	}

	@Override
	protected int pattern(GEP gep) {
		return 0;
	}

	@Override
	protected void examplfy(GEP gep, StringBuilder sb, int p) {

	}


}
