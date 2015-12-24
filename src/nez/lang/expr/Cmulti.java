package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.lang.Nez;

public class Cmulti extends Nez.String {

	Cmulti(SourceLocation s, boolean binary, byte[] byteSeq) {
		super(byteSeq);
		this.set(s);
	}

	@Override
	public boolean isConsumed() {
		return true;
	}

}
