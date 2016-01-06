package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.lang.Nez;

class Tcapture extends Nez.EndTree {

	Tcapture(SourceLocation s, int shift) {
		super(shift);
		this.setSourceLocation(s);
	}

}
