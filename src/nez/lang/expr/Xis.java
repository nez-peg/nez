package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.ast.Symbol;
import nez.lang.Expression;

public class Xis extends Unary implements Expression.Contextual {
	// final Grammar g;
	public final Symbol tableName;
	public final boolean is;

	Xis(SourceLocation s, NonTerminal pat, boolean is) {
		super(s, pat);
		this.tableName = Symbol.tag(pat.getLocalName());
		this.is = is;
	}

	Xis(SourceLocation s, Symbol tableName, Expression e, boolean is) {
		super(s, e);
		this.tableName = tableName;
		this.is = is;
	}

	@Override
	public final boolean equals(Object o) {
		if (o instanceof Xis) {
			Xis e = (Xis) o;
			return this.get(0).equals(e.get(0)) && this.tableName == e.tableName && this.is == e.is;
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
		return v.visitXis(this, a);
	}

	@Override
	public void format(StringBuilder sb) {
		if (this.is) {
			sb.append("<is ");
		} else {
			sb.append("<in ");
		}
		sb.append(getTableName());
		sb.append(">");
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