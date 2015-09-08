package nez.lang.expr;

import nez.ast.SourcePosition;
import nez.ast.Tag;
import nez.lang.Contextual;
import nez.lang.Expression;
import nez.lang.GrammarReshaper;
import nez.lang.PossibleAcceptance;
import nez.lang.Typestate;
import nez.lang.Visa;
import nez.vm.Instruction;
import nez.vm.NezEncoder;

public class MatchSymbol extends Expression implements Contextual {
	public final Tag tableName;

	MatchSymbol(SourcePosition s, Tag tableName) {
		super(s);
		this.tableName = tableName;
	}

	@Override
	public final boolean equalsExpression(Expression o) {
		if (o instanceof MatchSymbol) {
			MatchSymbol e = (MatchSymbol) o;
			return this.tableName == e.tableName;
		}
		return false;
	}

	public final Tag getTable() {
		return tableName;
	}

	public final String getTableName() {
		return tableName.getName();
	}

	@Override
	public String getPredicate() {
		return "match " + tableName.getName();
	}

	@Override
	public String key() {
		return this.getPredicate();
	}

	@Override
	public Expression reshape(GrammarReshaper m) {
		return m.reshapeMatchSymbol(this);
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
		return PossibleAcceptance.Accept;
	}

	@Override
	public Instruction encode(NezEncoder bc, Instruction next, Instruction failjump) {
		return bc.encodeMatchSymbol(this, next, failjump);
	}
}
