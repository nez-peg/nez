package nez.ext;

import nez.Parser;
import nez.debugger.DebugInputManager;
import nez.main.Command;
import nez.main.CommandContext;

public class Cndb extends Command {

	@Override
	public String getDesc() {
		return "Nez debugger";
	}

	@Override
	public void exec(CommandContext config) {
		Command.displayVersion();
		config.getOption().setOption("asis", true);
		config.getOption().setOption("intern", false);
		Parser parser = config.newParser();
		DebugInputManager manager = new DebugInputManager(config.inputFileLists);
		manager.exec(parser, config.getOption());
	}

}
