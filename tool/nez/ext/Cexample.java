package nez.ext;

import java.io.IOException;

import nez.lang.Grammar;
import nez.lang.GrammarExample;
import nez.main.Command;
import nez.main.CommandContext;

public class Cexample extends Command {
	@Override
	public void exec(CommandContext config) throws IOException {
		Grammar grammar = config.newGrammar();
		GrammarExample example = (GrammarExample) grammar.getMetaData("example");
		example.testAll(grammar, config.getStrategy(), true/* verbose */);
	}

	@Override
	public void exec() throws IOException {
		Grammar grammar = newGrammar();
		GrammarExample example = (GrammarExample) grammar.getMetaData("example");
		example.testAll(grammar, strategy, true/* verbose */);
	}

}