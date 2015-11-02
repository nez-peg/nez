package nez.ast.transducer;

import nez.ast.Tree;

public interface GrammarLoaderVisitor {
	public void accept(Tree<?> node);
}