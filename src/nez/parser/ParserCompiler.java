package nez.parser;

import nez.lang.Grammar;

public interface ParserCompiler {
	ParserCode<?> compile(Grammar grammar);
}
