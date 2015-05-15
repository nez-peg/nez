package nez.generator;

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
