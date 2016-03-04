package nez.bx;

import java.io.IOException;

import nez.lang.Grammar;

public class Command extends nez.main.Command {
	@Override
	public void exec() throws IOException {
		/* Setting requird options */
		strategy.Optimization = false;
		Grammar grammar = this.getSpecifiedGrammar();
		FormatGenerator gen = new FormatGenerator(outputDirectory, grammarFile);
		gen.generate(grammar);
	}

}
