package nez.main.ext;

import nez.cc.GrammarGenerator;

public class Llpeg {
	static {
		GrammarGenerator.regist("lpeg", nez.cc.LPegGrammarGenerator.class);
		// File Extension
		GrammarGenerator.regist(".lua", nez.cc.LPegGrammarGenerator.class);
	}
}
