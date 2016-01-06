package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.lang.Expression;
import nez.lang.Nez;

class Pone extends Nez.OneMore {
	Pone(SourceLocation s, Expression e) {
		super(e);
		this.setSourceLocation(s);
	}

}
