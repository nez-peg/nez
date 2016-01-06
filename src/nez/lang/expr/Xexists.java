package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.ast.Symbol;
import nez.lang.Expression;
import nez.lang.Nez;

class Xexists extends Nez.SymbolExists implements Expression.Contextual {

	Xexists(SourceLocation s, Symbol tableName, String symbol) {
		super(tableName, symbol);
		this.setSourceLocation(s);
	}

	public final String getSymbol() {
		return this.symbol;
	}

	public final Symbol getTable() {
		return tableName;
	}

	public final String getTableName() {
		return tableName.getSymbol();
	}

}
