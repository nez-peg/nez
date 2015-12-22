package nez.lang.schema;

import nez.ParserGenerator;
import nez.ParserGenerator.GrammarExtension;
import nez.ast.Tree;
import nez.lang.Grammar;
import nez.lang.ast.GrammarLoader;
import nez.lang.ast.GrammarLoaderExtension;

public class DTDGrammarExtension extends ParserGenerator.GrammarExtension {

	public DTDGrammarExtension() {
		super("xmldtd.nez");
	}

	@Override
	public GrammarExtension newState() {
		return new DTDGrammarExtension(); // in case of statefull
	}

	@Override
	public String getExtension() {
		return "dtd";
	}

	@Override
	public void updateGrammarLoader(GrammarLoader loader) {
		loader.add("Entity", new DTDEntity(loader));
		init(loader.getGrammar());
	}

	private void init(Grammar grammar) {

	}

	/*---------------------------------------------------------------------*/

	public class DTDElement extends GrammarLoaderExtension {
		public DTDElement(GrammarLoader loader) {
			super(loader);
		}

		@Override
		public void accept(Tree<?> node) {

		}
	}

	public class DTDEntity extends GrammarLoaderExtension {

		public DTDEntity(GrammarLoader loader) {
			super(loader);
			// TODO Auto-generated constructor stub
		}

		@Override
		public void accept(Tree<?> node) {
			// String entity = String.format("Entity_%s", id);
			// getGrammar().addProduction(null, entity, _String(value));
		}

	}
}
