package nez.lang;

import nez.SourceContext;
import nez.ast.CommonTree;
import nez.util.ConsoleUtils;

public class Example {
	String ruleName;
	CommonTree text;
	boolean result;
	Example(String ruleName, CommonTree text, boolean result) {
		this.ruleName = ruleName;
		this.text = text;
		this.result = result;
	}
	boolean test(NameSpace grammar) {
		Grammar p = grammar.newGrammar(this.ruleName, Grammar.ExampleOption);
		SourceContext c = text.newSourceContext();
		if(this.result) {
			ConsoleUtils.print("testing " + this.ruleName + " ");
			if(p.match(c)) {
				if(c.hasUnconsumed()) {
					ConsoleUtils.println("[UNCONSUMED]");
					ConsoleUtils.println(c.getUnconsumedMessage());
					return true;
				}
				ConsoleUtils.println("[PASS]");
				return true;
			}
			ConsoleUtils.println("[FAIL]");
			ConsoleUtils.println(c.getSyntaxErrorMessage());
		}
		else {
			ConsoleUtils.print("testing !" + this.ruleName + " ");
			if(!p.match(c)) {
				ConsoleUtils.println("[PASS]");
				return true;
			}
			ConsoleUtils.println("[FAIL]");
		}
		return false;
	}
}
