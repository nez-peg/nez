package nez.lang.expr;

import nez.ast.SourcePosition;
import nez.ast.Symbol;
import nez.lang.Expression;

import nez.lang.PossibleAcceptance;
import nez.lang.Typestate;
import nez.lang.Visa;

public class Xexists extends Term implements Expression.Contextual {
	public final Symbol tableName;
	String symbol;

	Xexists(SourcePosition s, Symbol tableName, String symbol) {
		super(s);
		this.tableName = tableName;
		this.symbol = symbol;
	}

	public final String getSymbol() {
		return this.symbol;
	}

	@Override
	public final boolean equalsExpression(Expression o) {
		if (o instanceof Xexists) {
			Xexists s = (Xexists) o;
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

	public final Symbol getTable() {
		return tableName;
	}

	public final String getTableName() {
		return tableName.getSymbol();
	}

	@Override
	public Object visit(Expression.Visitor v, Object a) {
		return v.visitXexists(this, a);
	}

	@Override
	public boolean isConsumed() {
		return false;
	}

	@Override
	public int inferTypestate(Visa v) {
		return Typestate.Unit;
	}

	@Override
	public short acceptByte(int ch) {
		return PossibleAcceptance.Unconsumed;
	}
}
