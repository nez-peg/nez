package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.lang.Nez;

class Treplace extends Nez.Replace {

	Treplace(SourceLocation s, String value) {
		super(value);
		this.setSourceLocation(s);
	}

}