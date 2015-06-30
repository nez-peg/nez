package nez.main.ext;

import nez.generator.GeneratorLoader;

public class Lcnez {
	static {
		GeneratorLoader.regist("cnez", nez.generator.CParserGenerator.class);
		// File Extension
		GeneratorLoader.regist(".c", nez.generator.CParserGenerator.class);
	}
}
