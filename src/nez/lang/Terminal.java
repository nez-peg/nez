package nez.lang;
import java.util.TreeMap;

import nez.ast.SourcePosition;
import nez.util.UMap;

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
	public int inferTypestate(UMap<String> visited) {
		return Typestate.BooleanType;
	}

}
