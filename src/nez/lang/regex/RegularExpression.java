package nez.lang.regex;

import nez.Grammar;
import nez.Parser;
import nez.Strategy;

public class RegularExpression {
	public final static Grammar newGrammar(String regex) {
		return newGrammar(regex, null);
	}

	public final static Grammar newGrammar(String regex, Strategy option) {
		RegularExpressionLoader l = new RegularExpressionLoader();
		Grammar g = new Grammar("re");
		l.eval(g, regex, 1, regex, option);
		return g;
	}

	public final static Parser newParser(String regex) {
		Strategy strategy = Strategy.newDefaultStrategy();
		Grammar g = newGrammar(regex, strategy);
		return g.newParser(strategy);
	}

}
