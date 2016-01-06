package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.lang.Nez;

class Tnew extends Nez.BeginTree {
	Tnew(SourceLocation s, int shift) {
		super(shift);
		this.setSourceLocation(s);
	}
}
