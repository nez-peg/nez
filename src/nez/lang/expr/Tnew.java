package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.lang.Nez;

public class Tnew extends Nez.PreNew {
	Tnew(SourceLocation s, int shift) {
		this.set(s);
		this.shift = shift;
	}

	@Override
	public boolean isConsumed() {
		return false;
	}

}
