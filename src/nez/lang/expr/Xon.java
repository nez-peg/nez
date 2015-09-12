package nez.lang.expr;

import nez.ast.SourcePosition;
import nez.lang.Conditional;
import nez.lang.Expression;
import nez.lang.GrammarTransducer;
import nez.lang.Visa;
import nez.parser.Instruction;
import nez.parser.NezEncoder;

public class Xon extends Unary implements Conditional {
	boolean predicate;

	public final boolean isPositive() {
		return predicate;
	}

	String flagName;

	public final String getFlagName() {
		return this.flagName;
	}

	Xon(SourcePosition s, boolean predicate, String flagName, Expression inner) {
		super(s, inner);
		if (flagName.startsWith("!")) {
			predicate = false;
			flagName = flagName.substring(1);
		}
		this.predicate = predicate;
		this.flagName = flagName;
	}

	@Override
	public final boolean equalsExpression(Expression o) {
		if (o instanceof Xon) {
			Xon e = (Xon) o;
			if (this.predicate == e.predicate && this.flagName.equals(e.flagName)) {
				return this.get(0).equalsExpression(e.get(0));
			}
			;
		}
		return false;
	}

	@Override
	public Expression reshape(GrammarTransducer m) {
		return m.reshapeXon(this);
	}

	@Override
	public boolean isConsumed() {
		return this.inner.isConsumed();
	}

	@Override
	public int inferTypestate(Visa v) {
		return this.inner.inferTypestate(v);
	}

	@Override
	public short acceptByte(int ch) {
		return this.inner.acceptByte(ch);
	}

	@Override
	public Instruction encode(NezEncoder bc, Instruction next, Instruction failjump) {
		return bc.encodeXon(this, next, failjump);
	}
}