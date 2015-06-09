package nez.main.ext;

import nez.generator.GeneratorLoader;

public class Lmouse {
	static {
		GeneratorLoader.regist("mouse", nez.generator.MouseGrammarGenerator.class);
		// File Extension
		GeneratorLoader.regist(".peg", nez.generator.MouseGrammarGenerator.class);
	}
}
