package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.lang.Expression;
import nez.lang.Nez;

class Tdetree extends Nez.Detree {
	Tdetree(SourceLocation s, Expression inner) {
		super(inner);
		this.setSourceLocation(s);
	}

}