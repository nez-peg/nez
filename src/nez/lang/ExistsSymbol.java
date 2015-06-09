package nez.lang;

import nez.ast.SourcePosition;
import nez.ast.Tag;
import nez.util.UList;
import nez.vm.Instruction;
import nez.vm.NezEncoder;

public class ExistsSymbol extends Expression implements Contextual {
	public final Tag tableName;
	final NameSpace ns;
	ExistsSymbol(SourcePosition s, NameSpace ns, Tag tableName) {
		super(s);
		this.ns = ns;
		this.tableName = tableName;
	}
	@Override
	public final boolean equalsExpression(Expression o) {
		if(o instanceof ExistsSymbol) {
			ExistsSymbol s = (ExistsSymbol)o;
			return this.ns == s.ns && this.tableName == s.tableName;
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

	public final Expression getSymbolExpression() {
		return ns.getSymbolExpresion(tableName.getName());
	}

	@Override
	public String getPredicate() {
		return "exists " + tableName.getName();
	}
	@Override
	public String key() {
		return this.getPredicate();
	}
	@Override
	public Expression reshape(GrammarReshaper m) {
		return m.reshapeExistsSymbol(this);
	}
	
	@Override
	public boolean isConsumed() {
		return false;
	}

	@Override
	public int inferTypestate(Visa v) {
		return Typestate.BooleanType;
	}
	
	@Override
	public short acceptByte(int ch, int option) {
		if(this.getSymbolExpression() != null) {
			return this.getSymbolExpression().acceptByte(ch, option);
		}
		return Acceptance.Accept;
	}
	@Override
	public Instruction encode(NezEncoder bc, Instruction next, Instruction failjump) {
		return bc.encodeExistsSymbol(this, next, failjump);
	}
	@Override
	protected int pattern(GEP gep) {
		return 1;
	}
	@Override
	protected void examplfy(GEP gep, StringBuilder sb, int p) {
		String token = gep.getSymbol(tableName);
		sb.append(token);
	}

}
