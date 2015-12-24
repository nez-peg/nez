package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.lang.Nez;

public class Pempty extends Nez.Empty {
	Pempty(SourceLocation s) {
		this.set(s);
	}

	@Override
	public boolean isConsumed() {
		return false;
	}

}
