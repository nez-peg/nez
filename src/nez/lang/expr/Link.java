package nez.lang.expr;

import nez.ast.SourcePosition;
import nez.ast.Tag;
import nez.lang.Expression;
import nez.lang.GrammarReshaper;
import nez.lang.Typestate;
import nez.lang.Visa;
import nez.vm.Instruction;
import nez.vm.NezEncoder;

public class Link extends Unary {
	@Deprecated
	public int index;
	Tag label;

	Link(SourcePosition s, Tag label, Expression e) {
		super(s, e);
		this.label = label;
	}

	public final Tag getLabel() {
		return this.label;
	}

	@Override
	public final boolean equalsExpression(Expression o) {
		if (o instanceof Link && this.label == ((Link) o).label) {
			return this.get(0).equalsExpression(o.get(0));
		}
		return false;
	}

	@Override
	public final void format(StringBuilder sb) {
		formatUnary(sb, (label != null) ? "$" + label + "(" : "$(", this.get(0), ")");
	}

	@Override
	public Expression reshape(GrammarReshaper m) {
		return m.reshapeLink(this);
	}

	@Override
	public boolean isConsumed() {
		return this.inner.isConsumed();
	}

	@Override
	public int inferTypestate(Visa v) {
		return Typestate.OperationType;
	}

	@Override
	public short acceptByte(int ch) {
		return inner.acceptByte(ch);
	}

	@Override
	public Instruction encode(NezEncoder bc, Instruction next, Instruction failjump) {
		return bc.encodeLink(this, next, failjump);
	}

}