package nez.lang.expr;

import nez.ast.SourcePosition;
import nez.lang.Typestate;
import nez.lang.Visa;

public abstract class Char extends Term {
	boolean binary;

	public final boolean isBinary() {
		return this.binary;
	}

	Char(SourcePosition s, boolean binary) {
		super(s);
		this.binary = binary;
	}

	@Override
	public int inferTypestate(Visa v) {
		return Typestate.Unit;
	}
}
