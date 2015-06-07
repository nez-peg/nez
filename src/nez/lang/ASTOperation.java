package nez.lang;

import java.util.TreeMap;

import nez.ast.SourcePosition;
import nez.util.UList;

public abstract class ASTOperation extends Expression {
	ASTOperation(SourcePosition s) {
		super(s);
	}
	@Override
	public int inferTypestate(Visa v) {
		return Typestate.OperationType;
	}
	@Override
	public short acceptByte(int ch, int option) {
		return Acceptance.Unconsumed;
	}
}
