package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.ast.Symbol;
import nez.lang.Nez;
import nez.lang.NonTerminal;
import nez.lang.Predicate;

public class Xsymbol extends Nez.SymbolAction {
	Xsymbol(SourceLocation s, NonTerminal pat) {
		super(Predicate.symbol, pat);
		this.setSourceLocation(s);
	}

	// Xsymbol(SourceLocation s, Symbol tableName, Expression pat) {
	// super(s, pat);
	// this.tableName = tableName;
	// }

	public final Symbol getTable() {
		return tableName;
	}

	public final String getTableName() {
		return tableName.getSymbol();
	}

}