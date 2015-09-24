package nez.main.ext;

import nez.x.generator.GeneratorLoader;

public class Lpegjs {
	static {
		GeneratorLoader.regist("pegjs", nez.x.generator.PegjsGrammarGenerator.class);
		// File Extension
		GeneratorLoader.regist(".pegjs", nez.x.generator.PegjsGrammarGenerator.class);
	}
}
