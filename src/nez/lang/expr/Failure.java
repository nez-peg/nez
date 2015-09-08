package nez.lang.expr;

import nez.ast.SourcePosition;
import nez.lang.Expression;
import nez.lang.GrammarReshaper;
import nez.lang.PossibleAcceptance;
import nez.lang.Typestate;
import nez.lang.Visa;
import nez.vm.Instruction;
import nez.vm.NezEncoder;

public class Failure extends Term {
	Failure(SourcePosition s) {
		super(s);
	}

	@Override
	public final boolean equalsExpression(Expression o) {
		return (o instanceof Failure);
	}

	@Override
	public final void format(StringBuilder sb) {
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
	public int inferTypestate(Visa v) {
		return Typestate.BooleanType;
	}

	@Override
	public short acceptByte(int ch) {
		return PossibleAcceptance.Reject;
	}

	@Override
	public Instruction encode(NezEncoder bc, Instruction next, Instruction failjump) {
		return bc.encodeFail(this);
	}

}