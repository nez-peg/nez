package nez.lang.ast;

import nez.ast.Tree;

public interface GrammarLoaderVisitor {
	public void accept(Tree<?> node);
}