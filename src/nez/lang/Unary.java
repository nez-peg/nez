package nez.lang;

import java.util.TreeMap;

import nez.ast.SourcePosition;

public abstract class Unary extends Expression {
	Expression inner;
	Unary(SourcePosition s, Expression inner) {
		super(s);
		this.inner = inner;
	}
	@Override
	public final int size() {
		return 1;
	}
	@Override
	public final Expression get(int index) {
		return this.inner;
	}
	@Override
	public final Expression set(int index, Expression e) {
		Expression old = this.inner;
		this.inner = e;
		return old;
	}
	@Override
	public String key() {
		return this.getPredicate();
	}

}
