package nez.lang.expr;

import nez.ast.SourcePosition;

public abstract class Char extends Term {
	boolean binary;

	public final boolean isBinary() {
		return this.binary;
	}

	Char(SourcePosition s, boolean binary) {
		super(s);
		this.binary = binary;
	}

}
