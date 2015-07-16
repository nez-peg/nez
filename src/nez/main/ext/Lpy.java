package nez.main.ext;

import nez.generator.GeneratorLoader;

public class Lpy {
	static {
		GeneratorLoader.regist("py", nez.generator.PythonParserGenerator.class);
		// File Extension
		GeneratorLoader.regist(".py", nez.generator.PythonParserGenerator.class);
	}
}
