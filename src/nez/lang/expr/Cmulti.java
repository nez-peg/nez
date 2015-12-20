package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.lang.Nez;
import nez.lang.PossibleAcceptance;

public class Cmulti extends Nez.String {

	Cmulti(SourceLocation s, boolean binary, byte[] byteSeq) {
		super(byteSeq);
		this.set(s);
	}

	@Override
	public boolean isConsumed() {
		return true;
	}

	@Override
	public short acceptByte(int ch) {
		return PossibleAcceptance.acceptByteChar(byteSeq[0] & 0xff, ch);
	}

}
