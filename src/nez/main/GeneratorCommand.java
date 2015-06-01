package nez.main;

import nez.generator.NezGenerator;
import nez.lang.Grammar;
import nez.lib.NezGrammar;

public class GeneratorCommand extends Command {

	@Override
	public String getDesc() {
		return "parser generator";
	}

	NezGenerator gen;

	public GeneratorCommand(NezGenerator gen) {
		this.gen = gen;
	}

	@Override
	public void exec(CommandConfigure config) {
		Grammar g = config.getGrammar();
		new NezGrammar();
		gen.generate(g);
	}

}
