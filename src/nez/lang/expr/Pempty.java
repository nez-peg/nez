package nez.lang.expr;

import nez.ast.SourcePosition;
import nez.lang.Expression;
import nez.lang.ExpressionVisitor;
import nez.lang.PossibleAcceptance;
import nez.lang.Typestate;
import nez.lang.Visa;
import nez.parser.AbstractGenerator;
import nez.parser.moz.MozInst;

public class Pempty extends Term {
	Pempty(SourcePosition s) {
		super(s);
	}

	@Override
	public final boolean equalsExpression(Expression o) {
		return (o instanceof Pempty);
	}

	@Override
	public final void format(StringBuilder sb) {
		sb.append("''");
	}

	@Override
	public Object visit(ExpressionVisitor v, Object a) {
		return v.visitPempty(this, a);
	}

	@Override
	public boolean isConsumed() {
		return false;
	}

	@Override
	public int inferTypestate(Visa v) {
		return Typestate.BooleanType;
	}

	@Override
	public short acceptByte(int ch) {
		return PossibleAcceptance.Unconsumed;
	}

	@Override
	public MozInst encode(AbstractGenerator bc, MozInst next, MozInst failjump) {
		return bc.encodePempty(this, next);
	}
}
