package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.lang.Bytes;
import nez.lang.Nez;

class Cset extends Nez.ByteSet {

	Cset(SourceLocation s, boolean binary, int beginChar, int endChar) {
		super();
		this.byteMap = Bytes.newMap(false);
		Bytes.appendRange(this.byteMap, beginChar, endChar);
	}

	Cset(SourceLocation s, boolean binary, boolean[] b) {
		super(b);
		this.byteMap = b;
	}
}
