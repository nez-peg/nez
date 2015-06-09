package nez.main.ext;

import nez.generator.GeneratorLoader;

public class Lpegtl {
	static {
		GeneratorLoader.regist("pegtl", nez.generator.PEGTLGenerator.class);
	}
}
