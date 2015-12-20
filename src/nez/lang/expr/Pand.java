package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.lang.Expression;
import nez.lang.Nez;
import nez.lang.PossibleAcceptance;

public class Pand extends Nez.And {
	Pand(SourceLocation s, Expression e) {
		super(e);
		this.set(s);
	}

	@Override
	public boolean isConsumed() {
		return false;
	}

	@Override
	public short acceptByte(int ch) {
		return PossibleAcceptance.acceptAnd(this, ch);
	}

}