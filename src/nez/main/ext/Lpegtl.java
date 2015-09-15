package nez.main.ext;

import nez.x.generator.GeneratorLoader;

public class Lpegtl {
	static {
		GeneratorLoader.regist("pegtl", nez.x.generator.PEGTLGenerator.class);
	}
}
