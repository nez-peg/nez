package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.lang.Nez;

public class Cmulti extends Nez.MultiByte {

	Cmulti(SourceLocation s, boolean binary, byte[] byteSeq) {
		super(byteSeq);
		this.setSourceLocation(s);
	}

	@Override
	public boolean isConsumed() {
		return true;
	}

}
