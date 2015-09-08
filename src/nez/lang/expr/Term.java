package nez.lang.expr;

import nez.ast.SourcePosition;
import nez.lang.Expression;

abstract class Term extends Expression {

	protected Term(SourcePosition s) {
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

}
