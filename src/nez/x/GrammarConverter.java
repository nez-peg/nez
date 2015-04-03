package nez.x;

import nez.Grammar;
import nez.ast.CommonTree;
import nez.ast.CommonTreeVisitor;
import nez.util.FileBuilder;

public abstract class GrammarConverter extends CommonTreeVisitor {
	final protected FileBuilder file;
	final protected Grammar grammar;
	public GrammarConverter(Grammar peg, String name) {
		this.file = new FileBuilder(name);
		this.grammar = peg;
	}
	public abstract String getDesc();
	public abstract void convert(CommonTree node);
}
