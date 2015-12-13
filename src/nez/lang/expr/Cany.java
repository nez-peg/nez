package nez.lang.expr;

import nez.ast.SourcePosition;
import nez.lang.Expression;
import nez.lang.ExpressionVisitor;
import nez.lang.PossibleAcceptance;
import nez.lang.Typestate;
import nez.lang.Visa;
import nez.parser.AbstractGenerator;
import nez.parser.moz.MozInst;

public class Cany extends Char {
	Cany(SourcePosition s, boolean binary) {
		super(s, binary);
	}

	@Override
	public final boolean equalsExpression(Expression o) {
		if (o instanceof Cany) {
			return this.binary == ((Cany) o).isBinary();
		}
		return false;
	}

	@Override
	public final void format(StringBuilder sb) {
		sb.append(".");
	}

	@Override
	public Object visit(ExpressionVisitor v, Object a) {
		return v.visitCany(this, a);
	}

	@Override
	public boolean isConsumed() {
		return true;
	}

	@Override
	public int inferTypestate(Visa v) {
		return Typestate.BooleanType;
	}

	@Override
	public short acceptByte(int ch) {
		return PossibleAcceptance.acceptAny(binary, ch);
	}

	@Override
	public MozInst encode(AbstractGenerator bc, MozInst next, MozInst failjump) {
		return bc.encodeCany(this, next, failjump);
	}
}