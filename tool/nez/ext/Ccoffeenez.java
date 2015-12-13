package nez.ext;

import nez.parser.GrammarWriter;

public class Ccoffeenez extends Cpeg {
	@Override
	protected GrammarWriter newParserGenerator(CommandContext config) {
		return config.newParserGenerator(nez.x.generator.CoffeeParserGenerator.class);
	}
}
