package nez.lang.expr;

import nez.ast.SourcePosition;
import nez.lang.Expression;
import nez.lang.Typestate;
import nez.lang.Visa;

public abstract class Char extends Expression {
	boolean binary;

	public final boolean isBinary() {
		return this.binary;
	}

	Char(SourcePosition s, boolean binary) {
		super(s);
		this.binary = binary;
	}

	@Override
	public final Expression get(int index) {
		return null;
	}

	@Override
	public final int size() {
		return 0;
	}

	@Override
	public int inferTypestate(Visa v) {
		return Typestate.BooleanType;
	}
}
