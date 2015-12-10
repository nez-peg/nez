package nez.ext;

import java.io.IOException;

import nez.lang.Grammar;
import nez.lang.GrammarExample;
import nez.main.Command;
import nez.main.CommandContext;

public class Ctest extends Command {
	@Override
	public void exec(CommandContext config) throws IOException {
		Grammar grammar = config.newGrammar();
		GrammarExample example = (GrammarExample) grammar.getMetaData("example");
		if (!example.testAll(grammar, config.getStrategy(), false/* verbose */)) {
			System.exit(1);
		}
	}
}
