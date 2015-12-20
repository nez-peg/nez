package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.lang.Nez;
import nez.lang.PossibleAcceptance;

public class Tnew extends Nez.PreNew {
	Tnew(SourceLocation s, int shift) {
		this.set(s);
		this.shift = shift;
	}

	@Override
	public boolean isConsumed() {
		return false;
	}

	@Override
	public short acceptByte(int ch) {
		return PossibleAcceptance.Unconsumed;
	}

}
