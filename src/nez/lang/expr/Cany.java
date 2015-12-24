package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.lang.Nez;

public class Cany extends Nez.Any {
	Cany(SourceLocation s, boolean binary) {
		this.set(s);
	}

	@Override
	public boolean isConsumed() {
		return true;
	}

}