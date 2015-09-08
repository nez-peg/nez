package nez.main;

import nez.SourceContext;
import nez.lang.Parser;

public class LCdebug extends Command {
	@Override
	public String getDesc() {
		return "Nez debugger";
	}

	@Override
	public void exec(CommandContext config) {
		// config.setNezOption(NezOption.DebugOption);
		Command.displayVersion();
		Parser peg = config.getGrammar();
		while (config.hasInputSource()) {
			SourceContext file = config.nextInputSource();
			peg.debug(file);
			file = null;
		}
	}
}