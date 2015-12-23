package nez.main;

import java.io.IOException;

import nez.lang.Grammar;
import nez.parser.Parser;
import nez.tool.parser.CParserGenerator;
import nez.tool.parser.CoffeeParserGenerator;
import nez.tool.parser.SourceGenerator;

public class Ccnez extends Command {

	@Override
	public void exec() throws IOException {
		SourceGenerator generator = newGenerator();
		Grammar g = newGrammar();
		Parser p = newParser();
		generator.init(g, p, g.getURN());
		generator.doc("cnez", g.getURN(), outputFormat);
		generator.generate();
	}

	protected SourceGenerator newGenerator() {
		if (outputFormat == null) {
			outputFormat = "c";
		}
		switch (outputFormat) {
		case "c":
			return new CParserGenerator();
		case "coffee":
			return new CoffeeParserGenerator();
		default:
			return (SourceGenerator) this.newExtendedOutputHandler("", "peg pegjs pegtl lpeg mouse nez");
		}
	}
}
