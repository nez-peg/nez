package nez.main.ext;

import nez.generator.NezGenerator;

public class Lmouse {
	static {
		NezGenerator.regist("mouse", nez.generator.MouseGrammarGenerator.class);
		// File Extension
		NezGenerator.regist(".peg", nez.generator.MouseGrammarGenerator.class);
	}
}
