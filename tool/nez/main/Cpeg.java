package nez.main;

import java.io.IOException;

import nez.lang.Grammar;
import nez.main.peg.GrammarTranslator;
import nez.main.peg.LPegTranslator;
import nez.main.peg.MouseTranslator;
import nez.main.peg.NezTranslator;
import nez.main.peg.PEGTLTranslator;
import nez.main.peg.PEGTranslator;
import nez.main.peg.PEGjsTranslator;
import nez.parser.Parser;

public class Cpeg extends Command {

	@Override
	public void exec() throws IOException {
		GrammarTranslator gw = newGrammarWriter();
		Grammar g = newGrammar();
		Parser p = newParser();
		gw.init(g, p, g.getURN());
		gw.pCommentLine("Translated by nez peg -g " + g.getURN() + " --format " + outputFormat);
		gw.generate(p.getParserGrammar());
	}

	protected GrammarTranslator newGrammarWriter() {
		if (outputFormat == null) {
			outputFormat = "nez";
		}
		switch (outputFormat) {
		case "nez":
			return new NezTranslator();
		case "peg":
			return new PEGTranslator();
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
			return (GrammarTranslator) this.newExtendedOutputHandler("");
		}
	}
}
