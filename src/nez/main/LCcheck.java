package nez.main;

import nez.SourceContext;
import nez.ast.CommonTree;
import nez.ast.CommonTreeWriter;
import nez.lang.Grammar;
import nez.lang.GrammarFile;
import nez.util.ConsoleUtils;

public class LCcheck extends Command {
	@Override
	public String getDesc() {
		return "a grammar checker";
	}

	@Override
	public void exec(CommandContext conf) {
		conf.getNezOption().setOption("example", true);
		GrammarFile gfile = conf.getGrammarFile(true);
		Grammar g = conf.getGrammar();
		while(conf.hasInputSource()) {
			SourceContext source = conf.nextInputSource();
			String urn = source.getResourceName();
			CommonTree node = g.parse(source);
			if(node == null) {
				ConsoleUtils.println(source.getSyntaxErrorMessage());
				continue;
			}
			if(source.hasUnconsumed()) {
				ConsoleUtils.println(source.getUnconsumedMessage());
			}

			String formatted = gfile.formatCommonTree(node);
			source = SourceContext.newStringSourceContext("(formatted)", 1, formatted);
			CommonTree node2 = g.parse(source);
			if(node2 == null) {
				ConsoleUtils.println(source.getSyntaxErrorMessage());
				continue;
			}
			if(source.hasUnconsumed()) {
				ConsoleUtils.println(source.getUnconsumedMessage());
			}
			String formatted2 = gfile.formatCommonTree(node2);
			if(!formatted.equals(formatted2)) {
				ConsoleUtils.println("[FAILED] mismatched " + urn);
			}
		}
	}
	
}
