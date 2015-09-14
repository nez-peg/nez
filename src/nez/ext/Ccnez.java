package nez.ext;

import java.io.IOException;

import nez.Parser;
import nez.main.Command;
import nez.main.CommandContext;
import nez.parser.ParserGenerator;

public class Ccnez extends Command {
	@Override
	public String getDesc() {
		return "an bytecode compiler";
	}

	@Override
	public void exec(CommandContext config) throws IOException {
		Parser p = config.newParser();
		ParserGenerator pgen = config.newParserGenerator(nez.parser.generator.CParserGenerator.class);
		pgen.generate(p.getGrammar());
	}

}
