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
}
