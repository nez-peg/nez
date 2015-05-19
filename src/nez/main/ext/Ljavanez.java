package nez.main.ext;

import nez.generator.NezGenerator;

public class Ljavanez {
	static {
		NezGenerator.regist("javanez", nez.generator.CombinatorGenerator.class);
	}
}
