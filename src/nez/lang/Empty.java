package nez.lang;

import nez.ast.SourcePosition;
import nez.vm.Instruction;
import nez.vm.NezEncoder;

public class Empty extends Unconsumed {
	Empty(SourcePosition s) {
		super(s);
	}
	@Override
	public final boolean equalsExpression(Expression o) {
		return (o instanceof Empty);
	}
	@Override
	protected final void format(StringBuilder sb) {
		sb.append("''");
	}
	@Override
	public String getPredicate() {
		return "empty";
	}
	@Override
	public String key() {
		return "";
	}
	@Override
	public Expression reshape(GrammarReshaper m) {
		return m.reshapeEmpty(this);
	}

	@Override
	public boolean isConsumed() {
		return false;
	}

	@Override
	public Instruction encode(NezEncoder bc, Instruction next, Instruction failjump) {
		return bc.encodeEmpty(this, next);
	}

	@Override
	protected int pattern(GEP gep) {
		return 0;
	}
	@Override
	protected void examplfy(GEP gep, StringBuilder sb, int p) {
	}

}
