package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.lang.Expression;
import nez.lang.Nez;

class Pnot extends Nez.Not {
	Pnot(SourceLocation s, Expression e) {
		super(e);
		this.setSourceLocation(s);
	}

}