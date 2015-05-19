package nez.lang;

import nez.ast.SourcePosition;
import nez.ast.Tag;
import nez.runtime.Instruction;
import nez.runtime.NezCompiler;
import nez.util.UList;
import nez.util.UMap;

public class LocalTable extends Unary {
	public final Tag tableName;
	public final NameSpace ns;
	
	LocalTable(SourcePosition s, NameSpace ns, Tag table, Expression inner) {
		super(s, inner);
		this.ns = ns;
		this.tableName = table;
		ns.setSymbolExpresion(tableName.getName(), inner);
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
	public Expression reshape(Manipulator m) {
		return m.reshapeLocalTable(this);
	}
	
	@Override
	public boolean isConsumed(Stacker stacker) {
		return this.inner.isConsumed(stacker);
	}

	@Override
	public boolean checkAlwaysConsumed(GrammarChecker checker, String startNonTerminal, UList<String> stack) {
		this.inner.checkAlwaysConsumed(checker, startNonTerminal, stack);
		return true;
	}
	
	@Override
	public int inferTypestate(UMap<String> visited) {
		return this.inner.inferTypestate(visited);
	}
	
	@Override
	public short acceptByte(int ch, int option) {
		return this.inner.acceptByte(ch, option);
	}
		
	@Override
	public Instruction encode(NezCompiler bc, Instruction next) {
		return bc.encodeLocalTable(this, next);
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
