package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.ast.Symbol;
import nez.lang.Nez;

public class Ttag extends Nez.Tag {

	Ttag(SourceLocation s, Symbol tag) {
		super(tag);
		this.setSourceLocation(s);
	}

	Ttag(SourceLocation s, String name) {
		this(s, Symbol.tag(name));
	}

	@Override
	public boolean isConsumed() {
		return false;
	}

}