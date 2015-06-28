package nez.main;

import nez.SourceContext;
import nez.ast.CommonTree;
import nez.ast.CommonTreeWriter;
import nez.lang.Grammar;
import nez.lang.GrammarChecker;
import nez.lang.GrammarFile;
import nez.lang.NezParser;
import nez.lang.Production;
import nez.util.ConsoleUtils;

public class LCformat extends Command {
	@Override
	public String getDesc() {
		return "a bi-directional parser";
	}

	@Override
	public void exec(CommandContext config) {
		GrammarFile gfile = config.getGrammarFile(false);
		Grammar g = config.getGrammar();
		while(config.hasInputSource()) {
			SourceContext source = config.nextInputSource();
			CommonTree node = g.parse(source);
			if(node == null) {
				ConsoleUtils.println(source.getSyntaxErrorMessage());
				continue;
			}
			if(source.hasUnconsumed()) {
				ConsoleUtils.println(source.getUnconsumedMessage());
			}
			source = null;
			g.logProfiler();
			ConsoleUtils.println(gfile.formatCommonTree(node));
		}
	}
}
