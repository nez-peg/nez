package nez.main.ext;

import nez.generator.GrammarGenerator;

public class Lnez {
	static {
		GrammarGenerator.regist("nez", nez.generator.NezGrammarGenerator.class);
		// File Extension
		GrammarGenerator.regist(".nez", nez.generator.NezGrammarGenerator.class);
	}
}
