package nez.ext;

import nez.main.CommandContext;
import nez.parser.ParserGenerator;

public class Ccoffeenez extends Cpeg {
	@Override
	protected ParserGenerator newParserGenerator(CommandContext config) {
		return config.newParserGenerator(nez.x.generator.CoffeeParserGenerator.class);
	}
}
