package nez.ext;

import java.io.IOException;

import nez.Parser;
import nez.main.Command;
import nez.main.CommandContext;
import nez.parser.ByteCoder;
import nez.parser.NezCode;
import nez.parser.NezCompiler;
import nez.parser.PackratCompiler;

public class Ccompile extends Command {
	@Override
	public String getDesc() {
		return "an bytecode compiler";
	}

	@Override
	public void exec(CommandContext config) throws IOException {
		Parser parser = config.newParser();
		NezCompiler compile = new PackratCompiler(config.getOption());
		ByteCoder c = new ByteCoder();
		NezCode code = compile.compile(parser.getGrammar(), c);
		c.writeTo(config.getGrammarFileName("nzc"));
	}

}
