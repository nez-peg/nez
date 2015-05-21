package nez.lang;

import nez.SourceContext;
import nez.ast.CommonTree;
import nez.main.Verbose;
import nez.util.ConsoleUtils;

public class Example {
	CommonTree nameNode;
	CommonTree textNode;
	boolean result;
	Example(CommonTree nameNode, CommonTree textNode, boolean result) {
		this.nameNode = nameNode;
		this.textNode = textNode;
		this.result = result;
	}
	
	boolean test(NameSpace grammar, int option) {
		Grammar p = grammar.newGrammar(nameNode.getText(), option);
		if(p == null) {
			System.out.println(nameNode.formatSourceMessage("error", "undefined nonterminal"));
			return false;
		}
		SourceContext c = textNode.newSourceContext();
		String name = (this.result ? "" : "!") + nameNode.getText() + 
				" (" + textNode.getSource().getResourceName() + ":" + textNode.getSourcePosition() + ")";
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

