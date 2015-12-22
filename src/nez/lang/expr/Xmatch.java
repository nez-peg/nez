package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.ast.Symbol;
import nez.lang.Expression;
import nez.lang.Nez;
import nez.lang.PossibleAcceptance;
import nez.lang.Predicate;

public class Xmatch extends Nez.SymbolPredicate implements Expression.Contextual {

	Xmatch(SourceLocation s, Symbol tableName) {
		super(Predicate.match, tableName);
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
		return false;
	}

	@Override
	public short acceptByte(int ch) {
		return PossibleAcceptance.Accept;
	}

}
