package nez.main.ext;

import nez.x.generator.GeneratorLoader;

public class Ljava {
	static {
		GeneratorLoader.regist("java", nez.x.generator.JavaParserGenerator.class);
		// File Extension
		GeneratorLoader.regist(".java", nez.x.generator.JavaParserGenerator.class);
	}

}
