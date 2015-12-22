package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.lang.Expression;
import nez.lang.PossibleAcceptance;

public class Xdefindent extends Term {
	Xdefindent(SourceLocation s) {
		super(s);
	}

	@Override
	public final boolean equals(Object o) {
		return (o instanceof Xsymbol);
	}

	@Override
	public boolean isConsumed() {
		return false;
	}

	@Override
	public short acceptByte(int ch) {
		return PossibleAcceptance.Unconsumed;
	}

	@Override
	public Object visit(Expression.Visitor v, Object a) {
		return v.visitUndefined(this, a);
	}

}