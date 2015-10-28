package nez.lang.schema;

import nez.lang.GrammarHacks;
import nez.lang.GrammarFile;
import nez.lang.expr.ExpressionCommons;
import nez.lang.expr.NonTerminal;

public abstract class AbstractPredefinedGrammar extends GrammarHacks {
	GrammarFile grammar;

	public AbstractPredefinedGrammar(GrammarFile grammar) {
		this.grammar = grammar;
	}

	abstract public void define();

	public NonTerminal _NonTerminal(String nonterminal) {
		return ExpressionCommons.newNonTerminal(null, grammar, nonterminal);
	}

}
