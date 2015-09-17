package nez.main.ext;

import nez.x.generator.GeneratorLoader;

public class Lcoffee {
	static {
		GeneratorLoader.regist("coffee", nez.x.generator.PythonParserGenerator.class);
		// File Extension
		GeneratorLoader.regist(".coffee", nez.x.generator.PythonParserGenerator.class);
	}
}
