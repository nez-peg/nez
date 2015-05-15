package nez.main.ext;

import nez.cc.GrammarGenerator;

public class Ljava {
	static {
		GrammarGenerator.regist("java", nez.cc.JavaParserGenerator.class);
		// File Extension
		GrammarGenerator.regist(".java", nez.cc.JavaParserGenerator.class);
	}

}
