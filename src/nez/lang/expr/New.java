package nez.lang.expr;

import nez.ast.SourcePosition;
import nez.ast.Tag;
import nez.lang.Expression;
import nez.lang.ExpressionTransducer;
import nez.lang.PossibleAcceptance;
import nez.lang.Typestate;
import nez.lang.Visa;
import nez.vm.Instruction;
import nez.vm.NezEncoder;

public class New extends Term {
	public boolean leftFold;
	Tag label;
	public Expression outer = null;
	public int shift = 0;

	New(SourcePosition s, boolean lefted, Tag label, int shift) {
		super(s);
		this.leftFold = lefted;
		this.label = label;
		this.shift = shift;
	}

	public Tag getLabel() {
		return this.label;
	}

	@Override
	public final boolean equalsExpression(Expression o) {
		if (o instanceof New) {
			New s = (New) o;
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
	public Expression reshape(ExpressionTransducer m) {
		return m.reshapeNew(this);
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
		return Typestate.ObjectType;
	}

	@Override
	public Instruction encode(NezEncoder bc, Instruction next, Instruction failjump) {
		return bc.encodeNew(this, next);
	}
}
