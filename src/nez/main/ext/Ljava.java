package nez.main.ext;

import nez.generator.GrammarGenerator;

public class Ljava {
	static {
		GrammarGenerator.regist("java", nez.generator.JavaParserGenerator.class);
		// File Extension
		GrammarGenerator.regist(".java", nez.generator.JavaParserGenerator.class);
	}

}
