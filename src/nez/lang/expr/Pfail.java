package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.lang.Nez;

public class Pfail extends Nez.Fail {
	Pfail(SourceLocation s) {
		this.setSourceLocation(s);
	}

	@Override
	public boolean isConsumed() {
		return true;
	}

}