package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.lang.Expression;
import nez.lang.Nez;

public class Tdetree extends Nez.Detree {
	Tdetree(SourceLocation s, Expression inner) {
		super(inner);
		this.set(s);
	}

	@Override
	public boolean isConsumed() {
		return this.inner.isConsumed();
	}

	@Override
	public short acceptByte(int ch) {
		return this.inner.acceptByte(ch);
	}

}