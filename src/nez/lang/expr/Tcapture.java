package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.lang.Nez;

public class Tcapture extends Nez.New {

	Tcapture(SourceLocation s, int shift) {
		this.setSourceLocation(s);
		this.shift = shift;
	}

	@Override
	public boolean isConsumed() {
		return false;
	}

}
