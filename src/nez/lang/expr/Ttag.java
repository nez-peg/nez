package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.ast.Symbol;
import nez.lang.Nez;
import nez.lang.PossibleAcceptance;

public class Ttag extends Nez.Tag {

	Ttag(SourceLocation s, Symbol tag) {
		super(tag);
		this.set(s);
	}

	Ttag(SourceLocation s, String name) {
		this(s, Symbol.tag(name));
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