package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.lang.Expression;
import nez.lang.PossibleAcceptance;

public class Xindent extends Term implements Expression.Contextual {
	Xindent(SourceLocation s) {
		super(s);
	}

	@Override
	public final boolean equals(Object o) {
		return (o instanceof Xindent);
	}

	@Override
	public Object visit(Expression.Visitor v, Object a) {
		return v.visitXindent(this, a);
	}

	@Override
	public boolean isConsumed() {
		return false;
	}

	@Override
	public short acceptByte(int ch) {
		if (ch == '\t' || ch == ' ') {
			return PossibleAcceptance.Accept;
		}
		return PossibleAcceptance.Unconsumed;
	}

}