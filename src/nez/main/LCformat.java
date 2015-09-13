package nez.main;

import java.io.IOException;


public class LCformat extends Command {
	@Override
	public String getDesc() {
		return "a bi-directional parser";
	}

	@Override
	public void exec(CommandContext config) throws IOException {
		// Grammar gfile = config.getGrammar(false);
		// Parser g = config.getGrammar();
		// while (config.hasInputSource()) {
		// SourceContext source = config.nextInputSource();
		// CommonTree node = g.parseCommonTree(source);
		// if (node == null) {
		// ConsoleUtils.println(source.getSyntaxErrorMessage());
		// continue;
		// }
		// if (source.hasUnconsumed()) {
		// ConsoleUtils.println(source.getUnconsumedMessage());
		// }
		// source = null;
		// ConsoleUtils.println(gfile.formatCommonTree(node));
		// g.logProfiler();
		// }
	}
}
