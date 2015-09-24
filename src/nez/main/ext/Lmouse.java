package nez.main.ext;

import nez.x.generator.GeneratorLoader;

public class Lmouse {
	static {
		GeneratorLoader.regist("mouse", nez.x.generator.MouseGrammarGenerator.class);
		// File Extension
		GeneratorLoader.regist(".peg", nez.x.generator.MouseGrammarGenerator.class);
	}
}
