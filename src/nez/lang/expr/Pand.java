package nez.lang.expr;

import nez.ast.SourcePosition;
import nez.lang.Expression;
import nez.lang.ExpressionVisitor;
import nez.lang.PossibleAcceptance;
import nez.lang.Typestate;
import nez.lang.Visa;
import nez.parser.AbstractGenerator;
import nez.parser.moz.MozInst;

public class Pand extends Unary {
	Pand(SourcePosition s, Expression e) {
		super(s, e);
	}

	@Override
	public final boolean equalsExpression(Expression o) {
		if (o instanceof Pand) {
			return this.get(0).equalsExpression(o.get(0));
		}
		return false;
	}

	@Override
	public final void format(StringBuilder sb) {
		this.formatUnary(sb, "&", this.inner, null);
	}

	@Override
	public Object visit(ExpressionVisitor v, Object a) {
		return v.visitPand(this, a);
	}

	@Override
	public boolean isConsumed() {
		return false;
	}

	@Override
	public int inferTypestate(Visa v) {
		int t = this.inner.inferTypestate(v);
		if (t == Typestate.ObjectType) { // typeCheck needs to report error
			return Typestate.BooleanType;
		}
		return t;
	}

	@Override
	public short acceptByte(int ch) {
		return PossibleAcceptance.acceptAnd(this, ch);
	}

	@Override
	public MozInst encode(AbstractGenerator bc, MozInst next, MozInst failjump) {
		return bc.encodePand(this, next, failjump);
	}

}