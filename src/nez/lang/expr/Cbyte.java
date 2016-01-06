package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.lang.Nez;

class Cbyte extends Nez.Byte {

	public Cbyte(SourceLocation s, boolean binary, int ch) {
		super(ch);
		this.setSourceLocation(s);
	}

}
