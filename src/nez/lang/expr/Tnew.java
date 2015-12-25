package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.lang.Nez;

public class Tnew extends Nez.PreNew {
	Tnew(SourceLocation s, int shift) {
		super(shift);
		this.setSourceLocation(s);
	}
}
