package nez.lang.schema;

import nez.ParserGenerator;
import nez.ast.Tree;
import nez.lang.Grammar;
import nez.lang.ast.GrammarLoader;
import nez.lang.ast.GrammarLoaderExtension;

public class CeleryGrammarExtension extends ParserGenerator.GrammarExtension {
	public CeleryGrammarExtension() {
		super("celery.nez");
	}

	// @Override
	// public GrammarExtension newState() {
	// return new CeleryGrammarExtension(); // in case of statefull
	// }

	@Override
	public String getExtension() {
		return "celery";
	}

	@Override
	public void updateGrammarLoader(GrammarLoader loader) {
		init(loader.getGrammar());
		loader.add("Celery", new Celery(loader));
		loader.add("Struct", new Struct(loader));

	}

	private void init(Grammar grammar) {
		generator = new JSONSchemaGrammarGenerator(grammar);
	}

	/*---------------------------------------------------------------------*/

	JSONSchemaGrammarGenerator generator;

	public class Celery extends GrammarLoaderExtension implements SchemaSymbol {

		public Celery(GrammarLoader loader) {
			super(loader);
		}

		@Override
		public void accept(Tree<?> node) {
			String rootStructName = node.get(0).getText(_Name, "");
			for (Tree<?> structNode : node) {
				loader.load(structNode);
			}
			generator.newRoot(rootStructName);
		}
	}

	public class Struct extends GrammarLoaderExtension implements SchemaSymbol {
		CelerySchemaConstructor constructor = new CelerySchemaConstructor(getGrammar(), getStrategy(), generator);

		public Struct(GrammarLoader loader) {
			super(loader);
		}

		@Override
		public void accept(Tree<?> node) {
			String structName = node.getText(_Name, "");
			constructor.setStructName(structName);
			generator.initMemberList();
			for (Tree<?> memberNode : node) {
				constructor.newSchema(memberNode);
			}
			generator.newSymbols();
			generator.genStruct(structName);
		}
	}
}
