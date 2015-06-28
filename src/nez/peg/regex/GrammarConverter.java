package nez.peg.regex;

import nez.ast.CommonTree;
import nez.ast.CommonTreeVisitor;
import nez.lang.GrammarFile;
import nez.util.FileBuilder;

public abstract class GrammarConverter extends CommonTreeVisitor {
	final protected FileBuilder file;
	final protected GrammarFile grammar;
	public GrammarConverter(GrammarFile peg, String name) {
		this.file = new FileBuilder(name);
		this.grammar = peg;
	}
	public abstract String getDesc();
	public abstract void convert(CommonTree node);
}
