package nez.main;

import nez.SourceContext;
import nez.ast.AbstractTreeWriter;
import nez.ast.CommonTree;
import nez.lang.Grammar;
import nez.util.ConsoleUtils;

public class LCjson extends Command {
	@Override
	public final String getDesc() {
		return "a JSON converter";
	}

	@Override
	public void exec(CommandContext config) {
		Grammar g = config.getGrammar();
		while (config.hasInputSource()) {
			SourceContext source = config.nextInputSource();
			CommonTree node = g.parseCommonTree(source);
			if (node == null) {
				ConsoleUtils.println(source.getSyntaxErrorMessage());
				continue;
			}
			if (source.hasUnconsumed()) {
				ConsoleUtils.println(source.getUnconsumedMessage());
			}
			g.logProfiler();
			AbstractTreeWriter w = new AbstractTreeWriter(config.getNezOption(), config.getOutputFileName(source, "json"));
			source = null;
			w.writeJSON(node);
			w.close();
		}
	}
}
