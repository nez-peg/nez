package nez.lang.expr;

import nez.ast.SourcePosition;
import nez.lang.Expression;
import nez.lang.ExpressionTransducer;
import nez.lang.Typestate;
import nez.lang.Visa;
import nez.vm.Instruction;
import nez.vm.NezEncoder;

public class Umatch extends Unary {
	Umatch(SourcePosition s, Expression inner) {
		super(s, inner);
	}

	@Override
	public final boolean equalsExpression(Expression o) {
		if (o instanceof Umatch) {
			return this.get(0).equalsExpression(o.get(0));
		}
		return false;
	}

	@Override
	public final void format(StringBuilder sb) {
		this.formatUnary(sb, "~", inner);
	}

	@Override
	public Expression reshape(ExpressionTransducer m) {
		return m.reshapeMatch(this);
	}

	@Override
	public boolean isConsumed() {
		return this.inner.isConsumed();
	}

	@Override
	public int inferTypestate(Visa v) {
		return Typestate.BooleanType;
	}

	@Override
	public short acceptByte(int ch) {
		return this.inner.acceptByte(ch);
	}

	@Override
	public Instruction encode(NezEncoder bc, Instruction next, Instruction failjump) {
		return this.inner.encode(bc, next, failjump);
	}

}