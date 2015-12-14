package nez.lang.expr;

import nez.ast.SourcePosition;
import nez.ast.Symbol;
import nez.lang.Expression;

import nez.lang.PossibleAcceptance;
import nez.lang.Typestate;
import nez.lang.Visa;

public class Xmatch extends Term implements Expression.Contextual {
	public final Symbol tableName;

	Xmatch(SourcePosition s, Symbol tableName) {
		super(s);
		this.tableName = tableName;
	}

	@Override
	public final boolean equalsExpression(Expression o) {
		if (o instanceof Xmatch) {
			Xmatch e = (Xmatch) o;
			return this.tableName == e.tableName;
		}
		return false;
	}

	public final Symbol getTable() {
		return tableName;
	}

	public final String getTableName() {
		return tableName.getSymbol();
	}

	@Override
	public Object visit(Expression.Visitor v, Object a) {
		return v.visitXmatch(this, a);
	}

	@Override
	public boolean isConsumed() {
		return false;
	}

	@Override
	public int inferTypestate(Visa v) {
		return Typestate.Unit;
	}

	@Override
	public short acceptByte(int ch) {
		return PossibleAcceptance.Accept;
	}

}
