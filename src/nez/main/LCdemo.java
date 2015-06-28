package nez.main;

import nez.SourceContext;
import nez.ast.CommonTree;
import nez.lang.Grammar;
import nez.lang.GrammarChecker;
import nez.lang.GrammarFile;
import nez.lang.NezParser;
import nez.lang.Production;
import nez.util.ConsoleUtils;

public class LCdemo extends Command {
	@Override
	public String getDesc() {
		return "Don't try this";
	}
	@Override
	public void exec(CommandContext config) {
	}
}
