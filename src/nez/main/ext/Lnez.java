package nez.main.ext;

import nez.x.generator.GeneratorLoader;

public class Lnez {
	static {
		GeneratorLoader.regist("nez", nez.parser.generator.NezGrammarGenerator.class);
		// File Extension
		GeneratorLoader.regist(".nez", nez.parser.generator.NezGrammarGenerator.class);
	}
}
