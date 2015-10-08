package nez.lang.schema;

import nez.lang.GrammarBase;
import nez.lang.GrammarFile;
import nez.lang.expr.ExpressionCommons;
import nez.lang.expr.NonTerminal;

public abstract class AbstractPredefinedGrammar extends GrammarBase {
	GrammarFile grammar;

	public AbstractPredefinedGrammar(GrammarFile grammar) {
		this.grammar = grammar;
	}

	abstract public void define();

	public NonTerminal _NonTerminal(String nonterminal) {
		return ExpressionCommons.newNonTerminal(null, grammar, nonterminal);
	}

}
