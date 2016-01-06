package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.ast.Symbol;
import nez.lang.Expression;
import nez.lang.Nez;

class Tlink extends Nez.LinkTree {

	Tlink(SourceLocation s, Symbol label, Expression e) {
		super(label, e);
	}

}