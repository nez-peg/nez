package nez.main.ext;

import nez.generator.GeneratorLoader;

public class Llpeg {
	static {
		GeneratorLoader.regist("lpeg", nez.generator.LPegGrammarGenerator.class);
		// File Extension
		GeneratorLoader.regist(".lua", nez.generator.LPegGrammarGenerator.class);
	}
}
