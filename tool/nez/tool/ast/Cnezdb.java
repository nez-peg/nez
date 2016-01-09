package nez.tool.ast;

import nez.main.Command;

public class Cnezdb extends Command {
	@Override
	public void exec() {
		Command.displayVersion();
		// config.getStrategy().Optimization = false;
		// Parser parser = config.newParser();
		// DebugManager manager = new DebugManager(config.inputFileLists);
		// manager.exec(parser, config.getStrategy());
	}
}
