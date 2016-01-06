package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.lang.Nez;

class Pempty extends Nez.Empty {
	Pempty(SourceLocation s) {
		this.setSourceLocation(s);
	}
}
