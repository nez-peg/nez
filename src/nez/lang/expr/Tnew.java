package nez.lang.expr;

import nez.ast.SourcePosition;
import nez.ast.SymbolId;
import nez.lang.Expression;
import nez.lang.GrammarTransducer;
import nez.lang.PossibleAcceptance;
import nez.lang.Typestate;
import nez.lang.Visa;
import nez.parser.Instruction;
import nez.parser.NezEncoder;

public class Tnew extends Term {
	public boolean leftFold;
	SymbolId label;
	public Expression outer = null;
	public int shift = 0;

	Tnew(SourcePosition s, boolean lefted, SymbolId label, int shift) {
		super(s);
		this.leftFold = lefted;
		this.label = label;
		this.shift = shift;
	}

	public SymbolId getLabel() {
		return this.label;
	}

	@Override
	public final boolean equalsExpression(Expression o) {
		if (o instanceof Tnew) {
			Tnew s = (Tnew) o;
			return (this.leftFold == s.leftFold && this.label == s.label && this.shift == s.shift);
		}
		return false;
	}

	@Override
	public final void format(StringBuilder sb) {
		sb.append("{");
		if (leftFold) {
			sb.append("$");
			if (label != null) {
				sb.append(label);
			}
		}
	}

	@Override
	public Expression reshape(GrammarTransducer m) {
		return m.reshapeTnew(this);
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
		return leftFold ? Typestate.OperationType : Typestate.ObjectType;
	}

	@Override
	public Instruction encode(NezEncoder bc, Instruction next, Instruction failjump) {
		return bc.encodeTnew(this, next);
	}
}
