package nez.main;

import java.io.IOException;

import nez.lang.Grammar;
import nez.parser.Parser;
import nez.parser.ParserCode;
import nez.parser.moz.MozCode;

public class Ccompile extends Command {
	@Override
	public void exec() throws IOException {
		Grammar grammar = newGrammar();
		Parser parser = newParser();
		String path = grammar.getURN();
		ParserCode<?> code = parser.compile();
		// String path = config.getGrammarName() + ".moz";
		MozCode.writeMozCode(parser, path + ".moz"); // FIXME
	}

}
