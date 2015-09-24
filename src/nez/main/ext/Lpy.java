package nez.main.ext;

import nez.x.generator.GeneratorLoader;

public class Lpy {
	static {
		GeneratorLoader.regist("py", nez.x.generator.PythonParserGenerator.class);
		// File Extension
		GeneratorLoader.regist(".py", nez.x.generator.PythonParserGenerator.class);
	}
}
