package nez.expr;

import nez.ast.SourcePosition;
import nez.ast.Tag;
import nez.runtime.Instruction;
import nez.runtime.RuntimeCompiler;
import nez.util.UList;
import nez.util.UMap;

public class IsSymbol extends Terminal {
	public Tag table;
	Expression symbolExpression = null;
	public boolean checkLastSymbolOnly = false;
	IsSymbol(SourcePosition s, boolean checkLastSymbolOnly, Tag table) {
		super(s);
		this.table = table;
		this.checkLastSymbolOnly = false;
	}
	public final Expression getSymbolExpression() {
		return this.symbolExpression;
	}
	@Override
	public String getPredicate() {
		return (checkLastSymbolOnly ? "is " : "isa ") + table.getName();
	}
	@Override
	public String getInterningKey() {
		return this.getPredicate();
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
	public Expression checkTypestate(GrammarChecker checker, Typestate c) {
		String tableName = table.getName();
		Expression e = checker.getSymbolExpresion(table.getName());
		if(e == null) {
			checker.reportError(this.s, "undefined table: " + tableName);
			return Factory.newFailure(this.s);
		}
		this.symbolExpression = e;
		return this;
	}
	@Override
	public short acceptByte(int ch, int option) {
		if(this.symbolExpression != null) {
			return this.symbolExpression.acceptByte(ch, option);
		}
		return Prediction.Accept;
	}
	@Override
	public Instruction encode(RuntimeCompiler bc, Instruction next) {
		return bc.encodeIsSymbol(this, next);
	}
	@Override
	protected int pattern(GEP gep) {
		return 1;
	}
	@Override
	protected void examplfy(GEP gep, StringBuilder sb, int p) {
		String token = gep.getSymbol(table);
		sb.append(token);
	}
}