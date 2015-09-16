package nez.ext;

import java.io.IOException;

import nez.Parser;
import nez.main.Command;
import nez.main.CommandContext;
import nez.main.Verbose;
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
		NezCompiler compile = new PackratCompiler(config.getStrategy());
		Verbose.println("generating .. " + config.getGrammarName() + ".moz");
		NezCode code = compile.compile(parser.getGrammar());
		ByteCoder c = new ByteCoder();
		code.encode(c);
		c.writeTo(config.getGrammarName() + ".moz");
	}

}
