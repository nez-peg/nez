package nez.peg.regex;

import nez.ast.CommonTree;
import nez.ast.CommonTreeVisitor;
import nez.lang.NameSpace;
import nez.util.FileBuilder;

public abstract class GrammarConverter extends CommonTreeVisitor {
	final protected FileBuilder file;
	final protected NameSpace grammar;
	public GrammarConverter(NameSpace peg, String name) {
		this.file = new FileBuilder(name);
		this.grammar = peg;
	}
	public abstract String getDesc();
	public abstract void convert(CommonTree node);
}
