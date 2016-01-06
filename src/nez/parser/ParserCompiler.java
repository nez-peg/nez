package nez.parser;

import nez.lang.Grammar;

public interface ParserCompiler {
	public ParserCode<?> compile(Grammar grammar);
}
