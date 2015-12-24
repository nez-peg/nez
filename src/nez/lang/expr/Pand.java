package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.lang.Expression;
import nez.lang.Nez;

public class Pand extends Nez.And {
	Pand(SourceLocation s, Expression e) {
		super(e);
		this.setSourceLocation(s);
	}

	@Override
	public boolean isConsumed() {
		return false;
	}

}