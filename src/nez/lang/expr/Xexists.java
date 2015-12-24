package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.ast.Symbol;
import nez.lang.Expression;
import nez.lang.Nez;

public class Xexists extends Nez.SymbolExists implements Expression.Contextual {

	Xexists(SourceLocation s, Symbol tableName, String symbol) {
		super(tableName, symbol);
		this.set(s);
	}

	public final java.lang.String getSymbol() {
		return this.symbol;
	}

	public final Symbol getTable() {
		return tableName;
	}

	public final java.lang.String getTableName() {
		return tableName.getSymbol();
	}

	@Override
	public boolean isConsumed() {
		return false;
	}

}
