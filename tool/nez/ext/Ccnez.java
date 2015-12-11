package nez.ext;

import nez.parser.ParserGenerator;

public class Ccnez extends Cpeg {
	protected ParserGenerator newParserGenerator(CommandContext config) {
		return config.newParserGenerator(nez.parser.generator.NezGrammarGenerator.class);
	}
}
