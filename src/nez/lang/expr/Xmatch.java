package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.ast.Symbol;
import nez.lang.Expression;
import nez.lang.Nez;
import nez.lang.Predicate;

public class Xmatch extends Nez.SymbolMatch implements Expression.Contextual {

	public Xmatch(SourceLocation s, Symbol tableName) {
		super(Predicate.match, tableName);
		this.setSourceLocation(s);
	}

	public final Symbol getTable() {
		return tableName;
	}

	public final String getTableName() {
		return tableName.getSymbol();
	}

}
