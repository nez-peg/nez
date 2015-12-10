package nez.ext;

import java.io.IOException;

import nez.lang.Example;
import nez.lang.Grammar;
import nez.lang.GrammarFile;
import nez.main.Command;
import nez.main.CommandContext;

public class Cexample extends Command {
	@Override
	public void exec(CommandContext config) throws IOException {
		Grammar g = config.newGrammar();
		if (g instanceof GrammarFile) {
			Example.testAll((GrammarFile) g, config.getStrategy(), true/*verbose*/);
		}
	}

}