package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.lang.Expression;
import nez.lang.Nez;

class Xblock extends Nez.BlockScope {
	Xblock(SourceLocation s, Expression e) {
		super(e);
		this.setSourceLocation(s);
	}

}