package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.lang.Nez;

public class Cbyte extends Nez.Byte {

	Cbyte(SourceLocation s, boolean binary, int ch) {
		super(ch);
		this.setSourceLocation(s);
	}

}
