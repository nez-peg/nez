package nez.main;

import nez.Parser;
import nez.parser.ByteCoder;
import nez.parser.NezCode;
import nez.parser.NezCompiler;
import nez.parser.PackratCompiler;

public class LCcompile extends Command {
	@Override
	public String getDesc() {
		return "an bytecode compiler";
	}

	@Override
	public void exec(CommandContext config) {
		Parser g = config.getGrammar();
		NezCompiler compile = new PackratCompiler(config.getNezOption());
		ByteCoder c = new ByteCoder();
		NezCode code = compile.compile(g, c);
		c.writeTo(config.getGrammarFileName("nzc"));
	}

}
