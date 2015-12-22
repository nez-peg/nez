package nez.lang.ast;

import nez.lang.Grammar;

public abstract class GrammarLoaderExtension implements GrammarLoaderVisitor {
	protected final GrammarLoader loader;

	public GrammarLoaderExtension(GrammarLoader loader) {
		this.loader = loader;
	}

	public final Grammar getGrammar() {
		return this.loader.getGrammar();
	}
}
