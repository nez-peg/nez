package nez.main.ext;

import nez.generator.GrammarGenerator;

public class Llpeg {
	static {
		GrammarGenerator.regist("lpeg", nez.generator.LPegGrammarGenerator.class);
		// File Extension
		GrammarGenerator.regist(".lua", nez.generator.LPegGrammarGenerator.class);
	}
}
