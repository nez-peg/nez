package nez.main.ext;

import nez.generator.GeneratorLoader;

public class Lscnez {
	static {
		GeneratorLoader.regist("scnez", nez.generator.SimpleCParserGenerator.class);
		// File Extension
		GeneratorLoader.regist(".c", nez.generator.SimpleCParserGenerator.class);
	}

}
