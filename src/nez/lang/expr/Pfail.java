package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.lang.Nez;

class Pfail extends Nez.Fail {
	Pfail(SourceLocation s) {
		this.setSourceLocation(s);
	}

}