package nez.lang;

import java.util.TreeMap;

import nez.ast.SourcePosition;
import nez.util.UList;

abstract class Unconsumed extends Expression {
	protected Unconsumed(SourcePosition s) {
		super(s);
	}
	@Override
	public final int size() {
		return 0;
	}
	@Override
	public final Expression get(int index) {
		return null;
	}
	@Override
	public String key() {
		return this.getPredicate();
	}
	@Override
	public int inferTypestate(Visa v) {
		return Typestate.BooleanType;
	}
	@Override
	public short acceptByte(int ch, int option) {
		return Acceptance.Unconsumed;
	}
}
