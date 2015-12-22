package nez.lang.schema;

import nez.ParserGenerator;
import nez.lang.ast.GrammarLoader;

public class DTDGrammarExtension extends ParserGenerator.GrammarExtension {

	DTDGrammarExtension() {
		super("lib/xmldtd.nez");
	}

	@Override
	public String getExtension() {
		return "dtd";
	}

	@Override
	public void updateGrammarLoader(GrammarLoader loader) {
		// TODO Auto-generated method stub
		// loader.add(, visitor);
	}

}
