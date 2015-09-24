package nez.main.ext;

import nez.x.generator.GeneratorLoader;

public class Llpeg {
	static {
		GeneratorLoader.regist("lpeg", nez.x.generator.LPegGrammarGenerator.class);
		// File Extension
		GeneratorLoader.regist(".lua", nez.x.generator.LPegGrammarGenerator.class);
	}
}
