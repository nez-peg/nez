package nez.ext;

import nez.parser.GrammarWriter;

public class Ccnez extends Cpeg {
	protected GrammarWriter newParserGenerator(CommandContext config) {
		return config.newParserGenerator(nez.parser.generator.NezGrammarGenerator.class);
	}
}
