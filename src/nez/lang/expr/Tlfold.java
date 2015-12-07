package nez.lang.expr;

import nez.ast.SourcePosition;
import nez.ast.Symbol;
import nez.lang.Expression;
import nez.lang.GrammarTransducer;
import nez.lang.PossibleAcceptance;
import nez.lang.Typestate;
import nez.lang.Visa;
import nez.parser.AbstractGenerator;
import nez.parser.moz.MozInst;

public class Tlfold extends Term {
	Symbol label;
	public Expression outer = null;
	public int shift = 0;

	Tlfold(SourcePosition s, Symbol label, int shift) {
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
	public Expression reshape(GrammarTransducer m) {
		return m.reshapeTlfold(this);
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

	@Override
	public int inferTypestate(Visa v) {
		return Typestate.OperationType;
	}

	@Override
	public MozInst encode(AbstractGenerator bc, MozInst next, MozInst failjump) {
		return bc.encodeTlfold(this, next);
	}
}
