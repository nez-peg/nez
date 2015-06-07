package nez.lang;

import nez.ast.SourcePosition;
import nez.ast.Tag;
import nez.util.UList;
import nez.vm.Instruction;
import nez.vm.NezEncoder;

public class IsSymbol extends Terminal implements Contextual {
	public final Tag tableName;
	final NameSpace ns;
	public final boolean checkLastSymbolOnly;
	IsSymbol(SourcePosition s, NameSpace ns, Tag tableName, boolean checkLastSymbolOnly) {
		super(s);
		this.ns = ns;
		this.tableName = tableName;
		this.checkLastSymbolOnly = false;
	}
	@Override
	public final boolean equalsExpression(Expression o) {
		if(o instanceof IsSymbol) {
			IsSymbol e = (IsSymbol)o;
			return this.tableName == e.tableName && this.ns == e.ns && this.checkLastSymbolOnly == e.checkLastSymbolOnly;
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
		return (checkLastSymbolOnly ? "is " : "isa ") + tableName.getName();
	}
	@Override
	public String key() {
		return this.getPredicate();
	}
	@Override
	public Expression reshape(GrammarReshaper m) {
		return m.reshapeIsSymbol(this);
	}
	
	@Override
	public boolean isConsumed() {
		Expression inner = this.getSymbolExpression();
		if(inner != null) {
			return inner.isConsumed();
		}
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
		return bc.encodeIsSymbol(this, next, failjump);
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