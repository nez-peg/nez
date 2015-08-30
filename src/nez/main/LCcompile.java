package nez.main;

import nez.lang.Grammar;
import nez.vm.ByteCoder;
import nez.vm.NezCode;
import nez.vm.NezCompiler;
import nez.vm.PackratCompiler;

public class LCcompile extends Command {
	@Override
	public String getDesc() {
		return "an bytecode compiler";
	}

	@Override
	public void exec(CommandContext config) {
		Grammar g = config.getGrammar();
		NezCompiler compile = new PackratCompiler(config.getNezOption());
		ByteCoder c = new ByteCoder();
		NezCode code = compile.compile(g, c);
		c.writeTo(config.getGrammarFileName("nzc"));
	}

}
