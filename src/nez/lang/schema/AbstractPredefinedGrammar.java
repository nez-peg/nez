package nez.lang.schema;

import nez.lang.GrammarFile;

public abstract class AbstractPredefinedGrammar {
	GrammarFile grammar;

	public AbstractPredefinedGrammar(GrammarFile grammar) {
		this.grammar = grammar;
	}

	abstract public void define();

}
