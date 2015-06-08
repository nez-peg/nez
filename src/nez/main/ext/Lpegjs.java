package nez.main.ext;

import nez.generator.GeneratorLoader;

public class Lpegjs {
	static {
		GeneratorLoader.regist("pegjs", nez.generator.PegjsGrammarGenerator.class);
		// File Extension
		GeneratorLoader.regist(".pegjs", nez.generator.PegjsGrammarGenerator.class);
	}
}
