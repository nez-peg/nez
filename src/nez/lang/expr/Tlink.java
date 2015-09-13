package nez.lang.expr;

import nez.ast.SourcePosition;
import nez.ast.SymbolId;
import nez.lang.Expression;
import nez.lang.GrammarTransducer;
import nez.lang.Typestate;
import nez.lang.Visa;
import nez.parser.Instruction;
import nez.parser.AbstractGenerator;

public class Tlink extends Unary {
	@Deprecated
	public int index = -1;
	SymbolId label;

	Tlink(SourcePosition s, SymbolId label, Expression e) {
		super(s, e);
		this.label = label;
	}

	public final SymbolId getLabel() {
		return this.label;
	}

	@Override
	public final boolean equalsExpression(Expression o) {
		if (o instanceof Tlink && this.label == ((Tlink) o).label) {
			return this.get(0).equalsExpression(o.get(0));
		}
		return false;
	}

	@Override
	public final void format(StringBuilder sb) {
		formatUnary(sb, (label != null) ? "$" + label + "(" : "$(", this.get(0), ")");
	}

	@Override
	public Expression reshape(GrammarTransducer m) {
		return m.reshapeTlink(this);
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
	public Instruction encode(AbstractGenerator bc, Instruction next, Instruction failjump) {
		return bc.encodeTlink(this, next, failjump);
	}

}