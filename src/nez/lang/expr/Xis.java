package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.ast.Symbol;
import nez.lang.Expression;
import nez.lang.Nez;
import nez.lang.NonTerminal;
import nez.lang.NezFunction;

public class Xis extends Nez.SymbolPredicate implements Expression.Contextual {
	// final Grammar g;

	public Xis(SourceLocation s, NonTerminal pat, boolean is) {
		super(is ? NezFunction.is : NezFunction.isa, Symbol.tag(pat.getLocalName()), pat);
		this.setSourceLocation(s);
	}

	public Xis(SourceLocation s, Symbol tableName, Expression e, boolean is) {
		super(is ? NezFunction.is : NezFunction.isa, tableName, e);
		this.setSourceLocation(s);
	}

	public final Symbol getTable() {
		return tableName;
	}

	public final String getTableName() {
		return tableName.getSymbol();
	}

}