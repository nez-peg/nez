package nez.lang;

import java.util.TreeMap;

import nez.ast.SourcePosition;
import nez.main.Command;
import nez.main.Verbose;
import nez.util.ConsoleUtils;
import nez.util.UList;
import nez.util.UMap;

public class GrammarChecker {
	
	boolean strictMode;
	int option;

	boolean foundError = false;
	boolean foundFlag  = false;

	public GrammarChecker(int checkerLevel) {
		this.strictMode = checkerLevel > 0;
	}
	
	public GrammarChecker(int checkerLevel, int option) {
		this.strictMode = checkerLevel > 0;
		this.option = option;
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

////UList<String> stack = new UList<String>(new String[64]);
////UMap<String> visited = new UMap<String>();
//for(Production r: grammar.getDefinedRuleList()) {
//	if(r.isTerminal()) {
//		continue;
//	}
//	if(AnalysisCache.hasRecursion(r)) {
//		r.setRecursive();
//		if(r.minlen > 0) {
//			continue;
//		}
//		r.minlen = -1;  // reset for all checking
//		r.isConsumed(new Stacker(r, null));
////		visited.clear();
////		r.checkAlwaysConsumed(this, null, stack);
//	}
////	if(r.minlen == -1) {
//////		visited.clear();
////		r.checkAlwaysConsumed(this, null, stack);
////	}
//}

	public void verify(NameSpace grammar) {
		NameAnalysis nameAnalyzer = new NameAnalysis();
		nameAnalyzer.analyze(grammar.getDefinedRuleList());
//		for(Production p: grammar.getDefinedRuleList()) {
//			nameAnalyzer.reshapeProduction(p);
//		}
//		for(Production r: grammar.getDefinedRuleList()) {
//			if(r.isTerminal()) {
//				continue;
//			}
//			if(AnalysisCache.hasRecursion(r)) {
//				r.setRecursive();
//				if(r.minlen > 0) {
//					continue;
//				}
//				r.minlen = -1;  // reset for all checking
//				r.isConsumed();
//			}
//		}
		if(this.foundError) {
			ConsoleUtils.exit(1, "FatalGrammarError");
		}
		// type check
		for(Production r: grammar.getRuleList()) {
			if(r.isTerminal()) {
				continue;
			}
			r.reshape(new Typestate(this));
		}		
		// interning
		if(this.option == Grammar.DebugOption) {
			for(Production r: grammar.getRuleList()) {
				GrammarFactory.setId(r.getExpression());
			}
		}
		else {
			for(Production r: grammar.getRuleList()) {
				if(r.isTerminal()) {
					continue;
				}
				if(Verbose.Grammar) {
					r.dump();
				}
				if(Command.ReleasePreview) {
					boolean r1 = r.isConditional();
					boolean r2 = r.testCondition(r.getExpression(), null);
					if(r1 != r2) {
						Verbose.FIXME("mismatch condition: " + r.getLocalName() + " " + r1 + " " + r2);
					}
				}
				if(Command.ReleasePreview) {
					boolean r1 = r.isContextual();
					boolean r2 = r.testContextSensitive(r.getExpression(), null);
					if(r1 != r2) {
						Verbose.FIXME("mismatch contextual: " + r.getLocalName() + " " + r1 + " " + r2);
					}
				}
				r.internRule();
			}
		}
		grammar.testExample(Grammar.ExampleOption);
	}
	
}
