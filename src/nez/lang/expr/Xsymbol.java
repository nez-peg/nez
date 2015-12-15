package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.ast.Symbol;
import nez.lang.Expression;

public class Xsymbol extends Unary {
	public final Symbol tableName;

	Xsymbol(SourceLocation s, NonTerminal pat) {
		super(s, pat);
		this.tableName = Symbol.tag(pat.getLocalName());
	}

	Xsymbol(SourceLocation s, Symbol tableName, Expression pat) {
		super(s, pat);
		this.tableName = tableName;
	}

	@Override
	public final boolean equalsExpression(Expression o) {
		if (o instanceof Xsymbol) {
			Xsymbol e = (Xsymbol) o;
			if (this.tableName == e.tableName) {
				return this.get(0).equalsExpression(e.get(0));
			}
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
	public void format(StringBuilder sb) {
		sb.append("<symbol ");
		sb.append(getTableName());
		sb.append(">");
	}

	@Override
	public Object visit(Expression.Visitor v, Object a) {
		return v.visitXdef(this, a);
	}

	@Override
	public boolean isConsumed() {
		return this.inner.isConsumed();
	}

	@Override
	public short acceptByte(int ch) {
		return this.inner.acceptByte(ch);
	}

}