package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.lang.Nez;
import nez.lang.PossibleAcceptance;

public class Cbyte extends Nez.Byte {

	Cbyte(SourceLocation s, boolean binary, int ch) {
		super(ch);
		this.set(s);
	}

	@Override
	public boolean isConsumed() {
		return true;
	}

	@Override
	public short acceptByte(int ch) {
		return PossibleAcceptance.acceptByteChar(byteChar, ch);
	}

}
