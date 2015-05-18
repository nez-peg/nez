package nez.main.ext;

import nez.generator.NezGenerator;

public class Lpegtl {
	static {
		NezGenerator.regist("pegtl", nez.generator.PEGTLGenerator.class);
	}
}
