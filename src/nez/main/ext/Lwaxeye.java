package nez.main.ext;

import nez.generator.GeneratorLoader;

public class Lwaxeye {
	static {
		GeneratorLoader.regist("waxeye", nez.generator.WaxeyeGenerator.class);
	}
}
