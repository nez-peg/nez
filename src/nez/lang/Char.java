package nez.lang;
import java.util.TreeMap;

import nez.ast.SourcePosition;

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
