package nez.main;

import java.io.IOException;

import nez.parser.Parser;
import nez.tool.parser.ParserGrammarWriter;
import nez.tool.peg.LPegTranslator;
import nez.tool.peg.PEGTranslator;

public class Cpeg extends Command {

	@Override
	public void exec() throws IOException {
		ParserGrammarWriter pw = newGenerator();
		// Grammar g = getSpecifiedGrammar();
		Parser p = newParser();
		pw.init(p);
		// generator.pCommentLine("Translated by nez peg -g " + g.getURN() +
		// " --format " + outputFormat);
		pw.generate();
	}

	protected ParserGrammarWriter newGenerator() {
		if (outputFormat == null) {
			outputFormat = "peg";
		}
		switch (outputFormat) {
		case "peg":
			return new PEGTranslator();
			// case "nez":
			// return new NezTranslator();
			// case "pegjs":
			// return new PEGjsTranslator();
			// case "pegtl":
			// return new PEGTLTranslator();
			// case "mouse":
			// return new MouseTranslator();
		case "lpeg":
		case "lua":
			return new LPegTranslator();
		default:
			return (ParserGrammarWriter) this.newExtendedOutputHandler("", "peg pegjs pegtl lpeg mouse nez");
		}
	}
}
