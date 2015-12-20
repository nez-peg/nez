package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.lang.Expression;
import nez.lang.Nez;
import nez.lang.PossibleAcceptance;

public class Pone extends Nez.OneMore {
	Pone(SourceLocation s, Expression e) {
		super(e);
		this.set(s);
	}

	@Override
	public boolean isConsumed() {
		return this.inner.isConsumed();
	}

	@Override
	public short acceptByte(int ch) {
		return PossibleAcceptance.acceptUnary(this, ch);
	}

}
