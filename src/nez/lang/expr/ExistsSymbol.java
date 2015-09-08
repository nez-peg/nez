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

public class ExistsSymbol extends Expression implements Contextual {
	public final Tag tableName;
	String symbol;

	ExistsSymbol(SourcePosition s, Tag tableName, String symbol) {
		super(s);
		this.tableName = tableName;
		this.symbol = symbol;
	}

	public final String getSymbol() {
		return this.symbol;
	}

	@Override
	public final boolean equalsExpression(Expression o) {
		if (o instanceof ExistsSymbol) {
			ExistsSymbol s = (ExistsSymbol) o;
			return this.tableName == s.tableName && equals(this.symbol, s.symbol);
		}
		return false;
	}

	private boolean equals(String s, String s2) {
		if (s != null && s2 != null) {
			return s.equals(s2);
		}
		return s == s2;
	}

	public final Tag getTable() {
		return tableName;
	}

	public final String getTableName() {
		return tableName.getName();
	}

	@Override
	public String getPredicate() {
		if (symbol == null) {
			return "exists " + tableName.getName();
		}
		return "exists " + tableName.getName() + " '" + this.symbol + "'";
	}

	@Override
	public String key() {
		return this.getPredicate();
	}

	@Override
	public Expression reshape(GrammarReshaper m) {
		return m.reshapeExistsSymbol(this);
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
		return bc.encodeExistsSymbol(this, next, failjump);
	}
}
