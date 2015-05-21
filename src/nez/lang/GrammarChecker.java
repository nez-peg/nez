package nez.lang;

import java.util.TreeMap;

import nez.ast.SourcePosition;
import nez.main.Verbose;
import nez.util.ConsoleUtils;
import nez.util.UList;
import nez.util.UMap;

public class GrammarChecker {
	
	boolean strictMode;

	boolean foundError = false;
	boolean foundFlag  = false;

	public GrammarChecker(int checkerLevel) {
		this.strictMode = checkerLevel > 0;
	}

	public GrammarChecker() {
		this.strictMode = false;
	}
	
	final void foundFlag() {
		this.foundFlag = true;
	}
	
	final void foundFatalError() {
		this.foundError = true;
	}
	
	public void reportError(SourcePosition s, String message) {
		if(s != null) {
			ConsoleUtils.println(s.formatSourceMessage("error", message));
		}
	}

	public void reportWarning(SourcePosition s, String message) {
		if(s != null) {
			ConsoleUtils.println(s.formatSourceMessage("warning", message));
		}
	}

	public void reportNotice(SourcePosition s, String message) {
		if(this.strictMode) {
			if(s != null) {
				ConsoleUtils.println(s.formatSourceMessage("notice", message));
			}
		}
	}

	public void exit(int exit, String message) {
		ConsoleUtils.exit(exit, message);
	}
	
	public final static boolean specialRuleName(String n) {
		return n.equalsIgnoreCase("FILE") || n.equalsIgnoreCase("CHUNK");
	}
	
	public void verify(NameSpace grammar) {
		NameAnalysis nameAnalyzer = new NameAnalysis();
		for(Production p: grammar.getDefinedRuleList()) {
			nameAnalyzer.reshapeProduction(p);
		}
		
//		UList<String> stack = new UList<String>(new String[64]);
//		UMap<String> visited = new UMap<String>();
		for(Production r: grammar.getDefinedRuleList()) {
			if(r.isTerminal) {
				continue;
			}
			if(AnalysisCache.hasRecursion(r)) {
				r.isRecursive = true;
				if(r.minlen > 0) {
					continue;
				}
				r.minlen = -1;  // reset for all checking
				r.isConsumed(new Stacker(r, null));
//				visited.clear();
//				r.checkAlwaysConsumed(this, null, stack);
			}
//			if(r.minlen == -1) {
////				visited.clear();
//				r.checkAlwaysConsumed(this, null, stack);
//			}
		}
		if(this.foundError) {
			ConsoleUtils.exit(1, "FatalGrammarError");
		}
		// type check
		for(Production r: grammar.getRuleList()) {
			if(r.isTerminal) {
				continue;
			}
//			this.checkPhase2(r.getExpression());
//			if(r.refCount == 0 && !r.isPublic && !specialRuleName(r.getLocalName())) {
//				this.reportWarning(r.s, "unused nonterminal definition");
//			}
			r.reshape(new Typestate(this));
		}		
		// interning
		for(Production r: grammar.getRuleList()) {
			if(r.isTerminal) {
				continue;
			}
			if(Verbose.Grammar) {
				r.dump();
			}
			r.internRule();
		}
//		if(this.foundFlag) {
//			TreeMap<String,String> undefedFlags = new TreeMap<String,String>();
//			for(Production r: grammar.getRuleList()) {
//				r.removeExpressionFlag(undefedFlags);
//			}
//		}
		grammar.testExample(Grammar.ExampleOption);
	}
	
//	void checkPhase1(Expression p, String ruleName, UMap<String> visited, int depth) {
//		p.checkPhase1(this, ruleName, visited, depth);
//		for(Expression e: p) {
//			this.checkPhase1(e, ruleName, visited, depth);
//		}
//	}
//
//	void checkPhase2(Expression p) {
//		p.checkPhase2(this);
//		for(Expression e: p) {
//			this.checkPhase2(e);
//		}
//	}
	
//	private UMap<Expression> tableMap; 
//
//	final void setSymbolExpresion(String tableName, Expression e) {
//		if(tableMap == null) {
//			tableMap = new UMap<Expression>();
//		}
//		tableMap.put(tableName, e);
//	}
//
//	final Expression getSymbolExpresion(String tableName) {
//		if(tableMap != null) {
//			return tableMap.get(tableName);
//		}
//		return null;
//	}
	
}
