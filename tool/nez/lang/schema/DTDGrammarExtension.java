package nez.lang.schema;

import nez.ParserGenerator;
import nez.ast.Tree;
import nez.lang.Grammar;
import nez.lang.ast.GrammarLoader;
import nez.lang.ast.GrammarLoaderExtension;

public class DTDGrammarExtension extends ParserGenerator.GrammarExtension {

	public DTDGrammarExtension() {
		super("xmldtd.nez");
	}

	// @Override
	// public GrammarExtension newState() {
	// return new DTDGrammarExtension(); // in case of statefull
	// }

	@Override
	public String getExtension() {
		return "dtd";
	}

	@Override
	public void updateGrammarLoader(GrammarLoader loader) {
		init(loader.getGrammar());
		loader.add("DTD", new DTD(loader));
		loader.add("Element", new DTDElement(loader));
		loader.add("Attlist", new DTDAttribute(loader));
		loader.add("Entity", new DTDEntity(loader));
	}

	private void init(Grammar grammar) {
		generator = new DTDSchemaGrammarGenerator(grammar);
	}

	/*---------------------------------------------------------------------*/

	DTDSchemaGrammarGenerator generator;

	public class DTD extends GrammarLoaderExtension implements SchemaSymbol {

		public DTD(GrammarLoader loader) {
			super(loader);
		}

		@Override
		public void accept(Tree<?> node) {
			String rootStructName = node.get(0).getText(_Name, "");
			for (Tree<?> subnode : node) {
				loader.load(subnode);
			}
			generator.genAllDTDElements();
			generator.newRoot(rootStructName);
			generator.newEntityList();
		}
	}

	public class DTDElement extends GrammarLoaderExtension implements SchemaSymbol {
		DTDSchemaConstructor constructor = new DTDSchemaConstructor(getGrammar(), getStrategy(), generator);

		public DTDElement(GrammarLoader loader) {
			super(loader);
		}

		@Override
		public void accept(Tree<?> node) {
			String elementName = node.getText(_Name, "");
			generator.addElementName(elementName);
			constructor.setElementName(elementName);
			generator.newMembers(String.format("%s_Contents", elementName), constructor.newSchema(node.get(_Member)));
		}
	}

	public class DTDAttribute extends GrammarLoaderExtension implements SchemaSymbol {
		DTDSchemaConstructor constructor = new DTDSchemaConstructor(getGrammar(), getStrategy(), generator);

		public DTDAttribute(GrammarLoader loader) {
			super(loader);
		}

		@Override
		public void accept(Tree<?> node) {
			String elementName = node.getText(_Name, "");
			constructor.setElementName(elementName);
			generator.initMemberList();
			for (Tree<?> subnode : node.get(_List)) {
				constructor.newSchema(subnode);
			}
			generator.newAttribute(elementName);
		}
	}

	public class DTDEntity extends GrammarLoaderExtension implements SchemaSymbol {
		public DTDEntity(GrammarLoader loader) {
			super(loader);
		}

		@Override
		public void accept(Tree<?> node) {
			generator.newEntity(node.getText(_Name, ""), node.getText(_Value, ""));
		}

	}

}
