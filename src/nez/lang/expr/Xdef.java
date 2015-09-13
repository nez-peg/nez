package nez.lang.expr;

import nez.Grammar;
import nez.ast.SourcePosition;
import nez.ast.SymbolId;
import nez.lang.Expression;
import nez.lang.GrammarTransducer;
import nez.lang.Typestate;
import nez.lang.Visa;
import nez.parser.Instruction;
import nez.parser.AbstractGenerator;
import nez.util.UMap;

public class Xdef extends Unary {
	public final SymbolId tableName;
	public final Grammar g;

	Xdef(SourcePosition s, Grammar g, SymbolId table, Expression inner) {
		super(s, inner);
		this.g = g;
		this.tableName = table;
		g.setSymbolExpresion(tableName.getSymbol(), inner);
	}

	@Override
	public final boolean equalsExpression(Expression o) {
		if (o instanceof Xdef) {
			Xdef e = (Xdef) o;
			if (this.tableName == e.tableName) {
				return this.get(0).equalsExpression(e.get(0));
			}
		}
		return false;
	}

	public final Grammar getGrammar() {
		return g;
	}

	public final SymbolId getTable() {
		return tableName;
	}

	public final String getTableName() {
		return tableName.getSymbol();
	}

	@Override
	public Expression reshape(GrammarTransducer m) {
		return m.reshapeXdef(this);
	}

	@Override
	public boolean isConsumed() {
		return this.inner.isConsumed();
	}

	@Override
	public int inferTypestate(Visa v) {
		return Typestate.BooleanType;
	}

	@Override
	public short acceptByte(int ch) {
		return this.inner.acceptByte(ch);
	}

	// Utilities
	public static boolean checkContextSensitivity(Expression e, UMap<String> visitedMap) {
		if (e.size() > 0) {
			for (int i = 0; i < e.size(); i++) {
				if (checkContextSensitivity(e.get(i), visitedMap)) {
					return true;
				}
			}
			return false;
		}
		if (e instanceof NonTerminal) {
			String un = ((NonTerminal) e).getUniqueName();
			if (visitedMap.get(un) == null) {
				visitedMap.put(un, un);
				return checkContextSensitivity(((NonTerminal) e).getProduction().getExpression(), visitedMap);
			}
			return false;
		}
		if (e instanceof Xindent || e instanceof Xis) {
			return true;
		}
		return false;
	}

	@Override
	public Instruction encode(AbstractGenerator bc, Instruction next, Instruction failjump) {
		return bc.encodeXdef(this, next, failjump);
	}

}