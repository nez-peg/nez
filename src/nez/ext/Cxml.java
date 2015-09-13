package nez.ext;

import java.io.IOException;

import nez.Parser;
import nez.SourceContext;
import nez.ast.AbstractTreeWriter;
import nez.ast.CommonTree;
import nez.main.Command;
import nez.main.CommandContext;
import nez.util.ConsoleUtils;

public class Cxml extends Command {
	@Override
	public final String getDesc() {
		return "an XML converter";
	}

	@Override
	public void exec(CommandContext config) throws IOException {
		Parser g = config.newParser();
		while (config.hasInput()) {
			SourceContext source = config.nextInput();
			CommonTree node = g.parseCommonTree(source);
			if (node == null) {
				ConsoleUtils.println(source.getSyntaxErrorMessage());
				continue;
			}
			if (source.hasUnconsumed()) {
				ConsoleUtils.println(source.getUnconsumedMessage());
			}
			g.logProfiler();
			AbstractTreeWriter w = new AbstractTreeWriter(config.getOption(), config.getOutputFileName(source, "xml"));
			source = null;
			w.writeXML(node);
			w.close();
		}
	}

}
