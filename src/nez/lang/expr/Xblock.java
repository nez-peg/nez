package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.lang.Expression;
import nez.lang.Nez;

public class Xblock extends Nez.BlockScope {
	Xblock(SourceLocation s, Expression e) {
		super(e);
		this.set(s);
	}

	@Override
	public boolean isConsumed() {
		return this.inner.isConsumed();
	}

}