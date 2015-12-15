package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.ast.Symbol;
import nez.lang.Expression;
import nez.lang.PossibleAcceptance;

public class Tlfold extends Term {
	Symbol label;
	public Expression outer = null;
	public int shift = 0;

	Tlfold(SourceLocation s, Symbol label, int shift) {
		super(s);
		this.label = label;
		this.shift = shift;
	}

	public Symbol getLabel() {
		return this.label;
	}

	@Override
	public final boolean equalsExpression(Expression o) {
		if (o instanceof Tlfold) {
			Tlfold s = (Tlfold) o;
			return (this.label == s.label && this.shift == s.shift);
		}
		return false;
	}

	@Override
	public final void format(StringBuilder sb) {
		sb.append("{$");
		if (label != null) {
			sb.append(label);
		}
	}

	@Override
	public Object visit(Expression.Visitor v, Object a) {
		return v.visitTlfold(this, a);
	}

	@Override
	public boolean isConsumed() {
		return false;
	}

	@Override
	public short acceptByte(int ch) {
		return PossibleAcceptance.Unconsumed;
	}

	// @Override
	// boolean setOuterLefted(Expression outer) {
	// if (this.leftFold) {
	// this.outer = outer;
	// return false;
	// }
	// return false;
	// }

}
