package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.lang.Nez;
import nez.lang.PossibleAcceptance;

public class Pfail extends Nez.Fail {
	Pfail(SourceLocation s) {
		this.set(s);
	}

	@Override
	public boolean isConsumed() {
		return true;
	}

	@Override
	public short acceptByte(int ch) {
		return PossibleAcceptance.Reject;
	}

}