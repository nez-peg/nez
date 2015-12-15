package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.ast.Symbol;
import nez.lang.Expression;

public class Tlink extends Unary {
	@Deprecated
	public int index = -1;
	Symbol label;

	Tlink(SourceLocation s, Symbol label, Expression e) {
		super(s, e);
		this.label = label;
	}

	public final Symbol getLabel() {
		return this.label;
	}

	@Override
	public final boolean equalsExpression(Expression o) {
		if (o instanceof Tlink && this.label == ((Tlink) o).label) {
			return this.get(0).equalsExpression(o.get(0));
		}
		return false;
	}

	@Override
	public final void format(StringBuilder sb) {
		formatUnary(sb, (label != null) ? "$" + label + "(" : "$(", this.get(0), ")");
	}

	@Override
	public Object visit(Expression.Visitor v, Object a) {
		return v.visitTlink(this, a);
	}

	@Override
	public boolean isConsumed() {
		return this.inner.isConsumed();
	}

	@Override
	public short acceptByte(int ch) {
		return inner.acceptByte(ch);
	}

}