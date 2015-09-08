package nez.lang.expr;

import nez.ast.SourcePosition;
import nez.lang.Conditional;
import nez.lang.Expression;
import nez.lang.GrammarReshaper;
import nez.lang.PossibleAcceptance;
import nez.lang.Typestate;
import nez.lang.Visa;
import nez.vm.Instruction;
import nez.vm.NezEncoder;

public class IfFlag extends Term implements Conditional {
	boolean predicate;
	String flagName;

	IfFlag(SourcePosition s, boolean predicate, String flagName) {
		super(s);
		if (flagName.startsWith("!")) {
			predicate = false;
			flagName = flagName.substring(1);
		}
		this.predicate = predicate;
		this.flagName = flagName;
	}

	@Override
	public final boolean equalsExpression(Expression o) {
		if (o instanceof IfFlag) {
			IfFlag e = (IfFlag) o;
			return this.predicate == e.predicate && this.flagName.equals(e.flagName);
		}
		return false;
	}

	public final String getFlagName() {
		return this.flagName;
	}

	public boolean isPredicate() {
		return predicate;
	}

	@Override
	public Expression reshape(GrammarReshaper m) {
		return m.reshapeIfFlag(this);
	}

	@Override
	public boolean isConsumed() {
		return false;
	}

	@Override
	public int inferTypestate(Visa v) {
		return Typestate.BooleanType;
	}

	@Override
	public short acceptByte(int ch) {
		return PossibleAcceptance.Unconsumed;
	}

	@Override
	public Instruction encode(NezEncoder bc, Instruction next, Instruction failjump) {
		return bc.encodeIfFlag(this, next, failjump);
	}

}