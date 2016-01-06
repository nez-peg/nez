package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.lang.Nez;

class Cany extends Nez.Any {
	Cany(SourceLocation s, boolean binary) {
		this.setSourceLocation(s);
	}

}