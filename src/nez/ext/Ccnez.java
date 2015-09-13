package nez.ext;

import java.io.IOException;

import nez.Parser;
import nez.generator.CParserGenerator;
import nez.main.Command;
import nez.main.CommandContext;

public class Ccnez extends Command {
	@Override
	public String getDesc() {
		return "an bytecode compiler";
	}

	@Override
	public void exec(CommandContext config) throws IOException {
		Parser p = config.newParser();
		CParserGenerator gen = new CParserGenerator();
		gen.generate(p, config.getOption(), null);
	}

}
