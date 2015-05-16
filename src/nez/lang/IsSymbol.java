package nez.lang;

import nez.ast.SourcePosition;
import nez.ast.Tag;
import nez.runtime.Instruction;
import nez.runtime.NezCompiler;
import nez.util.UList;
import nez.util.UMap;

public class IsSymbol extends Terminal {
	public final Tag tableName;
	final NameSpace ns;
	public final boolean checkLastSymbolOnly;
	IsSymbol(SourcePosition s, NameSpace ns, Tag tableName, boolean checkLastSymbolOnly) {
		super(s);
		this.ns = ns;
		this.tableName = tableName;
		this.checkLastSymbolOnly = false;
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
	public Expression reshape(Manipulator m) {
		return m.reshapeIsSymbol(this);
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
	public Instruction encode(NezCompiler bc, Instruction next) {
		return bc.encodeIsSymbol(this, next);
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