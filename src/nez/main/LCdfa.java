package nez.main;

import nez.lang.Grammar;
import nez.vm.DfaOptimizer;

class LCdfa extends Command {
	@Override
	public String getDesc() {
		return "DFA";
	}
	@Override
	public void exec(CommandContext config) {
		Grammar p = config.getGrammar();
		DfaOptimizer.optimize(p);
	}
}