package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.lang.Expression;
import nez.lang.Nez;

class Pzero extends Nez.ZeroMore {
	public boolean possibleInfiniteLoop = false;

	Pzero(SourceLocation s, Expression e) {
		super(e);
		this.setSourceLocation(s);
		// e.setOuterLefted(this);
	}

}