package nez.main.ext;

import nez.generator.GeneratorLoader;

public class Lc {
	static {
		GeneratorLoader.regist("c", nez.generator.CParserGenerator.class);
		// File Extension
		GeneratorLoader.regist(".c", nez.generator.CParserGenerator.class);
	}
}
