package nez.main.ext;

import nez.generator.NezGenerator;

public class Llpeg {
	static {
		NezGenerator.regist("lpeg", nez.generator.LPegGrammarGenerator.class);
		// File Extension
		NezGenerator.regist(".lua", nez.generator.LPegGrammarGenerator.class);
	}
}
