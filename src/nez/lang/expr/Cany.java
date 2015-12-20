package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.lang.Nez;
import nez.lang.PossibleAcceptance;

public class Cany extends Nez.Any {
	Cany(SourceLocation s, boolean binary) {
		this.set(s);
	}

	@Override
	public boolean isConsumed() {
		return true;
	}

	@Override
	public short acceptByte(int ch) {
		return PossibleAcceptance.acceptAny(false, ch);
	}

}