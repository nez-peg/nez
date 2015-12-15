package nez.lang.expr;

import nez.ast.SourceLocation;

public abstract class Char extends Term {
	boolean binary;

	public final boolean isBinary() {
		return this.binary;
	}

	Char(SourceLocation s, boolean binary) {
		super(s);
		this.binary = binary;
	}

}
