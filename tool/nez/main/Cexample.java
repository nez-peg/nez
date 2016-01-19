package nez.main;

import java.io.IOException;

import nez.lang.Grammar;
import nez.lang.ast.GrammarExample;

public class Cexample extends Ctest {

	@Override
	public void exec() throws IOException {
		Grammar grammar = newGrammar();
		GrammarExample example = (GrammarExample) grammar.getMetaData("example");
		testAll(grammar, example.getExampleList(), strategy);
	}

}