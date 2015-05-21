package nez.lang;

import nez.SourceContext;
import nez.ast.CommonTree;
import nez.main.Verbose;
import nez.util.ConsoleUtils;

public class Example {
	String localName;
	CommonTree text;
	boolean result;
	Example(String ruleName, CommonTree text, boolean result) {
		this.localName = ruleName;
		this.text = text;
		this.result = result;
	}
	
	boolean test(NameSpace grammar) {
		Grammar p = grammar.newGrammar(this.localName, Grammar.ExampleOption);
		SourceContext c = text.newSourceContext();
		String name = (this.result ? "" : "!") + this.localName + 
				" (" + text.getSource().getResourceName() + ":" + text.getSourcePosition() + ")";
//		if(Verbose.Example) {
//			Verbose.println("testing " + name + "...");
//		}
		boolean matchingResult = p.match(c);
		boolean unConsumed = true;
		if(matchingResult) {
			while(c.hasUnconsumed()) {
				int ch = c.byteAt(c.getPosition());
				if(ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r') {
					c.consume(1);
					continue;
				}
				break;
			}
			unConsumed = c.hasUnconsumed();
		}
		if(result) {
			if(!matchingResult) {
				Verbose.println("[FAIL] " + name);
				Verbose.println(c.getSyntaxErrorMessage());
				return false;
			}
			if(unConsumed) {
				Verbose.println("[FAIL] " + name);
				Verbose.println(c.getUnconsumedMessage());
				return false;
			}
			if(Verbose.Example) {
				Verbose.println("[PASS] " + name);
			}
			return true;
		}
		else {
			if(!matchingResult || unConsumed) {
				if(Verbose.Example) {
					Verbose.println("[PASS] " + name);
				}
				return true;
			}
			Verbose.println("[FAIL] " + name);
			return false;
		}
	}
}

