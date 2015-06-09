package nez.main.ext;

import nez.generator.GeneratorLoader;

public class Ljava {
	static {
		GeneratorLoader.regist("java", nez.generator.JavaParserGenerator.class);
		// File Extension
		GeneratorLoader.regist(".java", nez.generator.JavaParserGenerator.class);
	}

}
