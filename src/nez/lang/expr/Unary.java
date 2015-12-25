package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.lang.Expression;

public abstract class Unary extends Expressions {
	public Expression inner;

	Unary(SourceLocation s, Expression inner) {
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

}
