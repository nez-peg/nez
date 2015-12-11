package nez.lang.ast;

public abstract class GrammarLoaderExtension implements GrammarLoaderVisitor {
	protected final GrammarLoader loader;

	public GrammarLoaderExtension(GrammarLoader loader) {
		this.loader = loader;
	}
}
