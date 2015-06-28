package nez.main;

import nez.NezOption;
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
		//config.setNezOption(NezOption.DebugOption);
		Command.displayVersion();
		Grammar peg = config.getGrammar();
		while (config.hasInputSource()) {
			SourceContext file = config.nextInputSource();
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