package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.ast.Symbol;
import nez.lang.Nez;
import nez.lang.PossibleAcceptance;

public class Tlfold extends Nez.LeftFold {

	Tlfold(SourceLocation s, Symbol label, int shift) {
		super(shift, label);
		this.set(s);
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
