package nez.main.ext;

import nez.cc.GrammarGenerator;

public class Lnez {
	static {
		GrammarGenerator.regist("nez", nez.cc.NezGrammarGenerator.class);
		// File Extension
		GrammarGenerator.regist(".nez", nez.cc.NezGrammarGenerator.class);
	}
}
