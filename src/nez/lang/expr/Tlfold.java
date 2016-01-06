package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.ast.Symbol;
import nez.lang.Nez;

class Tlfold extends Nez.FoldTree {

	Tlfold(SourceLocation s, Symbol label, int shift) {
		super(shift, label);
		this.setSourceLocation(s);
	}

}
