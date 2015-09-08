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

public class Ttag extends Term {
	public Tag tag;

	Ttag(SourcePosition s, Tag tag) {
		super(s);
		this.tag = tag;
	}

	Ttag(SourcePosition s, String name) {
		this(s, Tag.tag(name));
	}

	public final String getTagName() {
		return tag.getName();
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
		sb.append("#" + tag.getName());
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
	public Expression reshape(ExpressionTransducer m) {
		return m.reshapeTtag(this);
	}

	@Override
	public Instruction encode(NezEncoder bc, Instruction next, Instruction failjump) {
		return bc.encodeTtag(this, next);
	}
}