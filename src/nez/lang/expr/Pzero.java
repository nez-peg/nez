package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.lang.Expression;
import nez.lang.Nez;

public class Pzero extends Nez.ZeroMore {
	public boolean possibleInfiniteLoop = false;

	Pzero(SourceLocation s, Expression e) {
		super(e);
		this.setSourceLocation(s);
		// e.setOuterLefted(this);
	}

	@Override
	public boolean isConsumed() {
		return false;
	}

}