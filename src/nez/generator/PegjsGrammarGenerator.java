package nez.generator;

import nez.lang.ByteChar;
import nez.lang.Choice;
import nez.lang.Expression;
import nez.lang.Production;
import nez.util.StringUtils;

public class PegjsGrammarGenerator extends GrammarGenerator {

	public PegjsGrammarGenerator() {
		super(null);
	}

	public PegjsGrammarGenerator(String fileName) {
		super(fileName);
	}

	@Override
	public String getDesc() {
		return "generate a PEGjs Grammar";
	}


}
