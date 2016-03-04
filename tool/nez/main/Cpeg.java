package nez.main;

import java.io.IOException;

import nez.lang.Grammar;
import nez.parser.Parser;
import nez.tool.parser.SourceGenerator;
import nez.tool.peg.LPegTranslator;
import nez.tool.peg.MouseTranslator;
import nez.tool.peg.NezTranslator;
import nez.tool.peg.PEGTLTranslator;
import nez.tool.peg.PEGTranslator;
import nez.tool.peg.PEGjsTranslator;

public class Cpeg extends Command {

	@Override
	public void exec() throws IOException {
		SourceGenerator generator = newGenerator();
		Grammar g = getSpecifiedGrammar();
		Parser p = newParser();
		generator.init(g, p, g.getURN());
		// generator.pCommentLine("Translated by nez peg -g " + g.getURN() +
		// " --format " + outputFormat);
		generator.generate();
	}

	protected SourceGenerator newGenerator() {
		if (outputFormat == null) {
			outputFormat = "peg";
		}
		switch (outputFormat) {
		case "peg":
			return new PEGTranslator();
		case "nez":
			return new NezTranslator();
		case "pegjs":
			return new PEGjsTranslator();
		case "pegtl":
			return new PEGTLTranslator();
		case "mouse":
			return new MouseTranslator();
		case "lpeg":
		case "lua":
			return new LPegTranslator();
		default:
			return (SourceGenerator) this.newExtendedOutputHandler("", "peg pegjs pegtl lpeg mouse nez");
		}
	}
}
