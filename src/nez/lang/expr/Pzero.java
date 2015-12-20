package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.lang.Expression;
import nez.lang.Nez;
import nez.lang.PossibleAcceptance;

public class Pzero extends Nez.ZeroMore {
	public boolean possibleInfiniteLoop = false;

	Pzero(SourceLocation s, Expression e) {
		super(e);
		this.set(s);
		// e.setOuterLefted(this);
	}

	@Override
	public boolean isConsumed() {
		return false;
	}

	@Override
	public short acceptByte(int ch) {
		return PossibleAcceptance.acceptOption(this, ch);
	}

}