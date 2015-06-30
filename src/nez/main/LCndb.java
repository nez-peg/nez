package nez.main;

import nez.SourceContext;
import nez.lang.Grammar;

public class LCndb extends Command {
	@Override
	public String getDesc() {
		return "Nez debugger";
	}
	@Override
	public void exec(CommandContext config) {
		//config.setNezOption(NezOption.DebugOption);
		Command.displayVersion();
		Grammar peg = config.getGrammar();
		while (config.hasInputSource()) {
			SourceContext file = config.nextInputSource();
			peg.debug(file);
			file = null;
		}
	}
}