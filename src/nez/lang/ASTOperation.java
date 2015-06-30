package nez.lang;

import nez.ast.SourcePosition;

public abstract class ASTOperation extends Expression {
	ASTOperation(SourcePosition s) {
		super(s);
	}
	@Override
	public int inferTypestate(Visa v) {
		return Typestate.OperationType;
	}
	@Override
	public short acceptByte(int ch) {
		return PossibleAcceptance.Unconsumed;
	}
}
