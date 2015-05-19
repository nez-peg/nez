package nez.main.ext;

import nez.generator.NezGenerator;

public class Ljava {
	static {
		NezGenerator.regist("java", nez.generator.ParserGenerator.class);
		// File Extension
		NezGenerator.regist(".java", nez.generator.ParserGenerator.class);
	}

}
