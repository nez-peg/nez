package nez.lang.expr;

import nez.ast.SourcePosition;
import nez.lang.Expression;
import nez.lang.ExpressionVisitor;
import nez.lang.PossibleAcceptance;
import nez.lang.Typestate;
import nez.lang.Visa;

public class Pzero extends Unary {
	public boolean possibleInfiniteLoop = false;

	Pzero(SourcePosition s, Expression e) {
		super(s, e);
		// e.setOuterLefted(this);
	}

	@Override
	public boolean equalsExpression(Expression o) {
		if (o instanceof Pzero) {
			return this.get(0).equalsExpression(o.get(0));
		}
		return false;
	}

	@Override
	public void format(StringBuilder sb) {
		this.formatUnary(sb, null, this.inner, "*");
	}

	@Override
	public Object visit(ExpressionVisitor v, Object a) {
		return v.visitPzero(this, a);
	}

	@Override
	public boolean isConsumed() {
		return false;
	}

	@Override
	public int inferTypestate(Visa v) {
		int t = this.inner.inferTypestate(v);
		if (t == Typestate.ObjectType) {
			return Typestate.BooleanType;
		}
		return t;
	}

	@Override
	public short acceptByte(int ch) {
		return PossibleAcceptance.acceptOption(this, ch);
	}

}