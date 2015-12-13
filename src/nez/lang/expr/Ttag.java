package nez.lang.expr;

import nez.ast.SourcePosition;
import nez.ast.Symbol;
import nez.lang.Expression;
import nez.lang.ExpressionVisitor;
import nez.lang.PossibleAcceptance;
import nez.lang.Typestate;
import nez.lang.Visa;
import nez.parser.AbstractGenerator;
import nez.parser.moz.MozInst;

public class Ttag extends Term {
	public Symbol tag;

	Ttag(SourcePosition s, Symbol tag) {
		super(s);
		this.tag = tag;
	}

	Ttag(SourcePosition s, String name) {
		this(s, Symbol.tag(name));
	}

	public final String getTagName() {
		return tag.getSymbol();
	}

	@Override
	public final boolean equalsExpression(Expression o) {
		if (o instanceof Ttag) {
			return this.tag == ((Ttag) o).tag;
		}
		return false;
	}

	@Override
	public final void format(StringBuilder sb) {
		sb.append("#" + tag.getSymbol());
	}

	@Override
	public boolean isConsumed() {
		return false;
	}

	@Override
	public int inferTypestate(Visa v) {
		return Typestate.OperationType;
	}

	@Override
	public short acceptByte(int ch) {
		return PossibleAcceptance.Unconsumed;
	}

	@Override
	public Object visit(ExpressionVisitor v, Object a) {
		return v.visitTtag(this, a);
	}

	@Override
	public MozInst encode(AbstractGenerator bc, MozInst next, MozInst failjump) {
		return bc.encodeTtag(this, next);
	}
}