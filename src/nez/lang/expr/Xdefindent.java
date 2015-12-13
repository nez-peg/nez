package nez.lang.expr;

import nez.ast.SourcePosition;
import nez.lang.Expression;
import nez.lang.ExpressionVisitor;
import nez.lang.PossibleAcceptance;
import nez.lang.Typestate;
import nez.lang.Visa;
import nez.parser.AbstractGenerator;
import nez.parser.moz.MozInst;

public class Xdefindent extends Term {
	Xdefindent(SourcePosition s) {
		super(s);
	}

	@Override
	public final boolean equalsExpression(Expression o) {
		return (o instanceof Xsymbol);
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
		return bc.encodeXdefindent(this, next, failjump);
	}

	@Override
	public Object visit(ExpressionVisitor v, Object a) {
		return v.visitUndefined(this, a);
	}

}