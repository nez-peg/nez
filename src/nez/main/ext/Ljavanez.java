package nez.main.ext;

import nez.x.generator.GeneratorLoader;

public class Ljavanez {
	static {
		GeneratorLoader.regist("javanez", nez.x.generator.CombinatorGenerator.class);
	}
}
