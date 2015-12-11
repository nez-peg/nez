package nez.main;

import java.io.IOException;

import nez.lang.Grammar;
import nez.lang.GrammarExample;

public class Cexample extends Command {

	@Override
	public void exec() throws IOException {
		Grammar grammar = newGrammar();
		GrammarExample example = (GrammarExample) grammar.getMetaData("example");
		example.testAll(grammar, strategy, true/* verbose */);
	}

}