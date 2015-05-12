package nez.x;

import nez.SourceContext;
import nez.ast.CommonTree;
import nez.lang.Grammar;
import nez.lang.GrammarChecker;
import nez.lang.NameSpace;
import nez.lang.NezParser;
import nez.lang.Production;
import nez.main.Command;
import nez.main.CommandConfigure;
import nez.util.ConsoleUtils;

public class DemoCommand extends Command {
	@Override
	public String getDesc() {
		return "Don't try this";
	}

	@Override
	public void exec(CommandConfigure config) {
	}
	
}
