package nez.main.ext;

import nez.generator.NezGenerator;

public class Lpegjs {
	static {
		NezGenerator.regist("pegjs", nez.generator.PegjsGrammarGenerator.class);
		// File Extension
		NezGenerator.regist(".pegjs", nez.generator.PegjsGrammarGenerator.class);
	}
}
