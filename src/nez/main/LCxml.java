package nez.main;

import nez.SourceContext;
import nez.ast.CommonTree;
import nez.ast.CommonTreeWriter;
import nez.lang.Grammar;
import nez.util.ConsoleUtils;

public class LCxml extends Command {
	@Override
	public final String getDesc() {
		return "an XML converter";
	}

	@Override
	public void exec(CommandContext config) {
		Grammar g = config.getGrammar();
		while(config.hasInputSource()) {
			SourceContext source = config.nextInputSource();
			CommonTree node = g.parseCommonTree(source);
			if(node == null) {
				ConsoleUtils.println(source.getSyntaxErrorMessage());
				continue;
			}
			if(source.hasUnconsumed()) {
				ConsoleUtils.println(source.getUnconsumedMessage());
			}
			source = null;
			g.logProfiler();
			CommonTreeWriter w = new CommonTreeWriter(config.getNezOption(),config.getOutputFileName(source, "xml"));
			w.writeXML(node);
			w.flush();
		}
	}
	
}
