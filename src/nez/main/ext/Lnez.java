package nez.main.ext;

import nez.generator.NezGenerator;

public class Lnez {
	static {
		NezGenerator.regist("nez", nez.generator.NezGrammarGenerator.class);
		// File Extension
		NezGenerator.regist(".nez", nez.generator.NezGrammarGenerator.class);
	}
}
