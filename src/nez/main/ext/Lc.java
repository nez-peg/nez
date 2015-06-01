package nez.main.ext;

import nez.generator.NezGenerator;

public class Lc {
	static {
		NezGenerator.regist("c", nez.generator.CParserGenerator.class);
		// File Extension
		NezGenerator.regist(".c", nez.generator.CParserGenerator.class);
	}
}
