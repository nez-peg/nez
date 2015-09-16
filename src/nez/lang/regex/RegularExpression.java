package nez.lang.regex;

import nez.Grammar;
import nez.Strategy;
import nez.Parser;
import nez.ast.Reporter;

public class RegularExpression {
	public final static Grammar newGrammar(String regex) {
		return newGrammar(regex, null, null);
	}

	public final static Grammar newGrammar(String regex, Strategy option, Reporter repo) {
		RegularExpressionLoader l = new RegularExpressionLoader();
		Grammar g = new Grammar("re");
		l.eval(g, regex, 1, regex, option, repo);
		return g;
	}

	public final static Parser newParser(String regex) {
		Strategy option = Strategy.newDefaultStrategy();
		Reporter repo = new Reporter();
		Grammar g = newGrammar(regex, option, repo);
		return g.newParser(option, repo);
	}

}
