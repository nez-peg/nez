package nez.main;

import nez.SourceContext;
import nez.ast.AbstractTreeWriter;
import nez.ast.CommonTree;
import nez.lang.Parser;
import nez.util.ConsoleUtils;

public class LCxml extends Command {
	@Override
	public final String getDesc() {
		return "an XML converter";
	}

	@Override
	public void exec(CommandContext config) {
		Parser g = config.getGrammar();
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
			AbstractTreeWriter w = new AbstractTreeWriter(config.getNezOption(), config.getOutputFileName(source, "xml"));
			source = null;
			w.writeXML(node);
			w.close();
		}
	}

}
