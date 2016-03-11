package nez.main;

import java.io.IOException;

import nez.lang.Grammar;
import nez.parser.Parser;
import nez.tool.parser.CParserGenerator;
import nez.tool.parser.JavaParserGenerator;
import nez.tool.parser.ParserGrammarWriter;
import nez.tool.parser.PythonParserGenerator;

public class Ccode extends Command {

	@Override
	public void exec() throws IOException {
		ParserGrammarWriter pw = newGenerator();
		Grammar g = getSpecifiedGrammar();
		Parser p = newParser();
		pw.init(p);
		pw.generate();
	}

	protected ParserGrammarWriter newGenerator() {
		if (outputFormat == null) {
			outputFormat = "c";
		}
		switch (outputFormat) {
		case "c":
			return new CParserGenerator();
		case "java":
			return new JavaParserGenerator();
		case "py":
		case "python":
			return new PythonParserGenerator();
			// case "coffee":
			// return new CoffeeParserGenerator();
		default:
			return (ParserGrammarWriter) this.newExtendedOutputHandler("", "c java python");
		}
	}
}
