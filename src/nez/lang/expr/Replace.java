package nez.lang.expr;

import nez.ast.SourcePosition;
import nez.lang.Expression;
import nez.lang.GrammarReshaper;
import nez.lang.PossibleAcceptance;
import nez.lang.Typestate;
import nez.lang.Visa;
import nez.util.StringUtils;
import nez.vm.Instruction;
import nez.vm.NezEncoder;

public class Replace extends Term {
	public String value;

	Replace(SourcePosition s, String value) {
		super(s);
		this.value = value;
	}

	@Override
	public final boolean equalsExpression(Expression o) {
		if (o instanceof Replace) {
			return this.value.equals(((Replace) o).value);
		}
		return false;
	}

	@Override
	public final void format(StringBuilder sb) {
		sb.append(StringUtils.quoteString('`', this.value, '`'));
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
	public Expression reshape(GrammarReshaper m) {
		return m.reshapeReplace(this);
	}

	@Override
	public Instruction encode(NezEncoder bc, Instruction next, Instruction failjump) {
		return bc.encodeReplace(this, next);
	}
}