package nez.main;

import java.io.IOException;

import nez.lang.Grammar;
import nez.lang.ast.GrammarExample;

public class Ctest extends Command {
	@Override
	public void exec() throws IOException {
		Grammar grammar = newGrammar();
		GrammarExample example = (GrammarExample) grammar.getMetaData("example");
		if (!example.testAll(grammar, strategy, false/* verbose */)) {
			System.exit(1);
		}
	}
}
