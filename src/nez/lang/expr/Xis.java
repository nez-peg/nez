package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.ast.Symbol;
import nez.lang.Expression;
import nez.lang.Nez;
import nez.lang.Predicate;

public class Xis extends Nez.SymbolPredicate implements Expression.Contextual {
	// final Grammar g;

	Xis(SourceLocation s, NonTerminal pat, boolean is) {
		super(is ? Predicate.is : Predicate.isa, Symbol.tag(pat.getLocalName()), pat);
		this.set(s);
	}

	Xis(SourceLocation s, Symbol tableName, Expression e, boolean is) {
		super(is ? Predicate.is : Predicate.isa, tableName, e);
		this.set(s);
	}

	public final Symbol getTable() {
		return tableName;
	}

	public final String getTableName() {
		return tableName.getSymbol();
	}

	@Override
	public boolean isConsumed() {
		return this.inner.isConsumed();
	}

}