package nez.lang;

import nez.ast.SourcePosition;
import nez.ast.Tag;
import nez.util.UList;
import nez.vm.Instruction;
import nez.vm.NezEncoder;

public class LocalTable extends Unary {
	public final Tag tableName;
	public final NameSpace ns;
	
	LocalTable(SourcePosition s, NameSpace ns, Tag table, Expression inner) {
		super(s, inner);
		this.ns = ns;
		this.tableName = table;
		ns.setSymbolExpresion(tableName.getName(), inner);
	}
	@Override
	public final boolean equalsExpression(Expression o) {
		if(o instanceof LocalTable) {
			LocalTable s = (LocalTable)o;
			if(this.ns == s.ns && this.tableName == s.tableName) {
				return this.get(0).equalsExpression(s.get(0));
			}
		}
		return false;
	}

	public final NameSpace getNameSpace() {
		return ns;
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
	public short acceptByte(int ch, int option) {
		return this.inner.acceptByte(ch, option);
	}
		
	@Override
	public Instruction encode(NezEncoder bc, Instruction next, Instruction failjump) {
		return bc.encodeLocalTable(this, next, failjump);
	}

	@Override
	protected int pattern(GEP gep) {
		return 1;
	}
	@Override
	protected void examplfy(GEP gep, StringBuilder sb, int p) {
		StringBuilder sb2 = new StringBuilder();
		inner.examplfy(gep, sb2, p);
		String token = sb2.toString();
		gep.addTable(tableName, token);
		sb.append(token);
	}
}
