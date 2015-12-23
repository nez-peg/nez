package nez.lang.schema;

import nez.ast.Tree;
import nez.lang.Grammar;
import nez.parser.ParserStrategy;

public class DTDSchemaConstructor extends SchemaConstructor {
	public DTDSchemaConstructor(Grammar grammar, ParserStrategy strategy) {
		super(grammar, strategy);
		init(DTDSchemaConstructor.class, new Undefined());
	}

	public class Undefined implements SchemaTransducer {

		@Override
		public Schema accept(Tree<?> node) {
			undefined(node);
			return null;
		}

	}
}
