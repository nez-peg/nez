package nez.lang.ast;

import nez.ast.Tree;

public interface GrammarLoaderVisitor {
	void accept(Tree<?> node);
}