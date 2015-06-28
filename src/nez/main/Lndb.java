package nez.main;

import nez.GrammarOption;
import nez.SourceContext;
import nez.ast.CommonTree;
import nez.ast.CommonTreeWriter;
import nez.lang.Grammar;
import nez.util.ConsoleUtils;

public class Lndb extends Command {
	@Override
	public String getDesc() {
		return "Nez debugger";
	}
	@Override
	public void exec(CommandContext config) {
		//config.setGrammarOption(GrammarOption.DebugOption);
		Command.displayVersion();
		Grammar peg = config.getGrammar();
		while (config.hasInput()) {
			SourceContext file = config.getInputSourceContext();
			CommonTree node = (CommonTree) peg.parse(file);
			if(node == null) {
				ConsoleUtils.println(file.getSyntaxErrorMessage());
				continue;
			}
			if(file.hasUnconsumed()) {
				ConsoleUtils.println(file.getUnconsumedMessage());
			}
			file = null;
			new CommonTreeWriter().transform(config.getOutputFileName(file), node);
		}
	}
	
}