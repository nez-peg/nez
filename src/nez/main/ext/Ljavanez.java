package nez.main.ext;

import nez.generator.GeneratorLoader;

public class Ljavanez {
	static {
		GeneratorLoader.regist("javanez", nez.generator.CombinatorGenerator.class);
	}
}
