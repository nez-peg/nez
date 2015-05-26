package nez.lang;

import nez.ast.SourcePosition;
import nez.ast.Tag;
import nez.util.UList;
import nez.util.UMap;
import nez.vm.Instruction;
import nez.vm.NezCompiler;

public class DefSymbol extends Unary {
	public final Tag tableName;
	public final NameSpace ns;
	
	DefSymbol(SourcePosition s, NameSpace ns, Tag table, Expression inner) {
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
		return "def " + tableName.getName();
	}
	
	@Override
	public String key() {
		return "def " + tableName.getName();
	}

	@Override
	public Expression reshape(GrammarReshaper m) {
		return m.reshapeDefSymbol(this);
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
		return Typestate.BooleanType;
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
				return checkContextSensitivity(((NonTerminal) e).getProduction().getExpression(), visitedMap);
			}
			return false;
		}
		if(e instanceof IsIndent || e instanceof IsSymbol) {
			return true;
		}
		return false;
	}
	
	@Override
	public Instruction encode(NezCompiler bc, Instruction next, Instruction failjump) {
		return bc.encodeDefSymbol(this, next, failjump);
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