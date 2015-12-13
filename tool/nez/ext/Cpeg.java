package nez.ext;

import java.io.IOException;

import nez.main.Command;
import nez.parser.Parser;
import nez.parser.GrammarWriter;

public class Cpeg extends Command {

	@Override
	public void exec(CommandContext config) throws IOException {
		GrammarWriter pgen = newParserGenerator(config);
		Parser p = config.newParser();
		pgen.generate(p.getParserGrammar());
	}

	protected GrammarWriter newParserGenerator(CommandContext config) {
		return config.newParserGenerator(nez.parser.generator.PEGGenerator.class);
	}
}
