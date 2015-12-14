package nez.lang.expr;

import nez.ast.SourcePosition;
import nez.ast.Symbol;
import nez.lang.Expression;

public class Xlocal extends Unary {
	public final Symbol tableName;

	Xlocal(SourcePosition s, Symbol table, Expression inner) {
		super(s, inner);
		this.tableName = table;
	}

	@Override
	public final boolean equalsExpression(Expression o) {
		if (o instanceof Xlocal) {
			Xlocal s = (Xlocal) o;
			if (this.tableName == s.tableName) {
				return this.get(0).equalsExpression(s.get(0));
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
	public Object visit(Expression.Visitor v, Object a) {
		return v.visitXlocal(this, a);
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
