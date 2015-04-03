package nez.expr;

import nez.ast.SourcePosition;
import nez.ast.Tag;
import nez.runtime.Instruction;
import nez.runtime.RuntimeCompiler;
import nez.util.UList;
import nez.util.UMap;

public class DefSymbol extends Unary {
	public final Tag table;
	DefSymbol(SourcePosition s, Tag table, Expression inner) {
		super(s, inner);
		this.table = table;
	}
	@Override
	public String getPredicate() {
		return "def " + table.name;
	}
	@Override
	public String getInterningKey() {
		return "def " + table.getName();
	}
	@Override
	public boolean checkAlwaysConsumed(GrammarChecker checker, String startNonTerminal, UList<String> stack) {
		this.inner.checkAlwaysConsumed(checker, startNonTerminal, stack);
		return true;
	}
	@Override void checkPhase1(GrammarChecker checker, String ruleName, UMap<String> visited, int depth) {
		checker.setSymbolExpresion(table.getName(), this.inner);
	}
	@Override void checkPhase2(GrammarChecker checker) {
		if(!this.inner.isAlwaysConsumed()) {
			checker.reportWarning(s, "possible zero-length symbol: " + this.inner);
		}
	}
	@Override
	public int inferTypestate(UMap<String> visited) {
		return Typestate.BooleanType;
	}
	@Override
	public Expression checkTypestate(GrammarChecker checker, Typestate c) {
		int t = this.inner.inferTypestate(null);
		if(t != Typestate.BooleanType) {
			this.inner = this.inner.removeASTOperator(Expression.CreateNonTerminal);
		}
		return this;
	}
	@Override
	Expression dupUnary(Expression e) {
		return (this.inner != e) ? Factory.newDefSymbol(this.s, this.table, e) : this;
	}
	@Override
	public short acceptByte(int ch, int option) {
		return this.inner.acceptByte(ch, option);
	}
	
	// Utilities
	public static boolean checkContextSensitivity(Expression e, UMap<String> visitedMap) {
		if(e.size() > 0) {
			for(int i = 0; i < e.size(); i++) {
				if(checkContextSensitivity(e.get(i), visitedMap)) {
					return true;
				}
			}
			return false;
		}
		if(e instanceof NonTerminal) {
			String un = ((NonTerminal) e).getUniqueName();
			if(visitedMap.get(un) == null) {
				visitedMap.put(un, un);
				return checkContextSensitivity(((NonTerminal) e).getRule().getExpression(), visitedMap);
			}
			return false;
		}
		if(e instanceof IsIndent || e instanceof IsSymbol) {
			return true;
		}
		return false;
	}
	
	@Override
	public Instruction encode(RuntimeCompiler bc, Instruction next) {
		return bc.encodeDefSymbol(this, next);
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
		gep.addTable(table, token);
		sb.append(token);
	}
	
}