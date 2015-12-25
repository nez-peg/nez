package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.lang.Expression;

abstract class Term extends Expressions {

	protected Term(SourceLocation s) {
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
