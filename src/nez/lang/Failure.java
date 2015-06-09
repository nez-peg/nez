package nez.lang;

import nez.ast.SourcePosition;
import nez.vm.Instruction;
import nez.vm.NezEncoder;

public class Failure extends Unconsumed {
	Failure(SourcePosition s) {
		super(s);
	}
	@Override
	public final boolean equalsExpression(Expression o) {
		return (o instanceof Failure);
	}
	@Override
	protected final void format(StringBuilder sb) {
		sb.append("!''");
	}
	@Override
	public String getPredicate() {
		return "fail";
	}
	@Override
	public String key() {
		return "!!";
	}
	@Override
	public Expression reshape(GrammarReshaper m) {
		return m.reshapeFailure(this);
	}
	@Override
	public boolean isConsumed() {
		return true;
	}

	@Override
	public short acceptByte(int ch, int option) {
		return Acceptance.Reject;
	}
	@Override
	public Instruction encode(NezEncoder bc, Instruction next, Instruction failjump) {
		return bc.encodeFail(this);
	}
	@Override
	protected int pattern(GEP gep) {
		return 1;
	}

	@Override
	protected void examplfy(GEP gep, StringBuilder sb, int p) {
		sb.append("\0");
	}

}