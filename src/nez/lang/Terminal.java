package nez.lang;
import java.util.TreeMap;

import nez.ast.SourcePosition;

public abstract class Terminal extends Expression {
	Terminal(SourcePosition s) {
		super(s);
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
