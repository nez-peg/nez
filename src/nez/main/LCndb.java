package nez.main;

import nez.NezOption;
import nez.SourceContext;
import nez.ast.CommonTree;
import nez.ast.CommonTreeWriter;
import nez.lang.Grammar;
import nez.util.ConsoleUtils;

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