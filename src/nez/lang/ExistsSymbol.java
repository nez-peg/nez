package nez.lang;

import nez.ast.SourcePosition;
import nez.ast.Tag;
import nez.util.UList;
import nez.util.UMap;
import nez.vm.Instruction;
import nez.vm.NezCompiler;

public class ExistsSymbol extends Expression {
	public final Tag tableName;
	final NameSpace ns;
	
	ExistsSymbol(SourcePosition s, NameSpace ns, Tag tableName) {
		super(s);
		this.ns = ns;
		this.tableName = tableName;
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
	public boolean isConsumed(Stacker stacker) {
		Expression inner = this.getSymbolExpression();
		if(inner != null) {
			return inner.isConsumed(stacker);
		}
		return false;
	}

	@Override
	public boolean checkAlwaysConsumed(GrammarChecker checker, String startNonTerminal, UList<String> stack) {
		return true;
	}
	@Override
	public int inferTypestate(UMap<String> visited) {
		return Typestate.BooleanType;
	}
	@Override
	public short acceptByte(int ch, int option) {
		if(this.getSymbolExpression() != null) {
			return this.getSymbolExpression().acceptByte(ch, option);
		}
		return Prediction.Accept;
	}
	@Override
	public Instruction encode(NezCompiler bc, Instruction next, Instruction failjump) {
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
