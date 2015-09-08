package nez.lang.expr;

import nez.ast.SourcePosition;
import nez.ast.Tag;
import nez.lang.Expression;
import nez.lang.GrammarMap;
import nez.lang.ExpressionTransducer;
import nez.lang.Typestate;
import nez.lang.Visa;
import nez.util.UMap;
import nez.vm.Instruction;
import nez.vm.NezEncoder;

public class DefSymbol extends Unary {
	public final Tag tableName;
	public final GrammarMap g;

	DefSymbol(SourcePosition s, GrammarMap g, Tag table, Expression inner) {
		super(s, inner);
		this.g = g;
		this.tableName = table;
		g.setSymbolExpresion(tableName.getName(), inner);
	}

	@Override
	public final boolean equalsExpression(Expression o) {
		if (o instanceof DefSymbol) {
			DefSymbol e = (DefSymbol) o;
			if (this.tableName == e.tableName) {
				return this.get(0).equalsExpression(e.get(0));
			}
		}
		return false;
	}

	public final GrammarMap getGrammarFile() {
		return g;
	}

	public final Tag getTable() {
		return tableName;
	}

	public final String getTableName() {
		return tableName.getName();
	}

	@Override
	public Expression reshape(ExpressionTransducer m) {
		return m.reshapeDefSymbol(this);
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
		if (e instanceof IsIndent || e instanceof IsSymbol) {
			return true;
		}
		return false;
	}

	@Override
	public Instruction encode(NezEncoder bc, Instruction next, Instruction failjump) {
		return bc.encodeDefSymbol(this, next, failjump);
	}

}