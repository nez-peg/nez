package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.lang.Expression;
import nez.lang.Nez;
import nez.lang.PossibleAcceptance;

public class Poption extends Nez.Option {
	Poption(SourceLocation s, Expression e) {
		super(e);
		this.set(s);
	}

	@Override
	public boolean isConsumed() {
		return false;
	}

	@Override
	public short acceptByte(int ch) {
		return PossibleAcceptance.acceptOption(this, ch);
	}

}