package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.ast.Symbol;
import nez.lang.Expression;
import nez.lang.Nez;

class Xlocal extends Nez.LocalScope {

	Xlocal(SourceLocation s, Symbol table, Expression inner) {
		super(table, inner);
		this.setSourceLocation(s);
	}

	public final Symbol getTable() {
		return tableName;
	}

	public final String getTableName() {
		return tableName.getSymbol();
	}

}
