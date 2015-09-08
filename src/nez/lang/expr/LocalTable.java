package nez.lang.expr;

import nez.ast.SourcePosition;
import nez.ast.Tag;
import nez.lang.Expression;
import nez.lang.GrammarReshaper;
import nez.lang.Visa;
import nez.vm.Instruction;
import nez.vm.NezEncoder;

public class LocalTable extends Unary {
	public final Tag tableName;

	LocalTable(SourcePosition s, Tag table, Expression inner) {
		super(s, inner);
		this.tableName = table;
	}

	@Override
	public final boolean equalsExpression(Expression o) {
		if (o instanceof LocalTable) {
			LocalTable s = (LocalTable) o;
			if (this.tableName == s.tableName) {
				return this.get(0).equalsExpression(s.get(0));
			}
		}
		return false;
	}

	public final Tag getTable() {
		return tableName;
	}

	public final String getTableName() {
		return tableName.getName();
	}

	@Override
	public String getPredicate() {
		return "local " + tableName.getName();
	}

	@Override
	public String key() {
		return "local " + tableName.getName();
	}

	@Override
	public Expression reshape(GrammarReshaper m) {
		return m.reshapeLocalTable(this);
	}

	@Override
	public boolean isConsumed() {
		return this.inner.isConsumed();
	}

	@Override
	public int inferTypestate(Visa v) {
		return this.inner.inferTypestate(v);
	}

	@Override
	public short acceptByte(int ch) {
		return this.inner.acceptByte(ch);
	}

	@Override
	public Instruction encode(NezEncoder bc, Instruction next, Instruction failjump) {
		return bc.encodeLocalTable(this, next, failjump);
	}

}
