package nez.lang.schema;

import nez.ast.Symbol;
import nez.ast.Tree;
import nez.lang.Grammar;
import nez.lang.ast.GrammarVisitorMap;
import nez.parser.ParserStrategy;

public abstract class SchemaConstructor extends GrammarVisitorMap<SchemaTransducer> {
	public SchemaConstructor(Grammar grammar, ParserStrategy strategy) {
		super(grammar, strategy);
	}

	public Schema newSchema(Tree<?> node) {
		return this.find(key(node)).accept(node);
	}

}

interface SchemaTransducer {
	Schema accept(Tree<?> node);
}

interface SchemaSymbol {
	Symbol _Key = Symbol.tag("key");
	Symbol _Value = Symbol.tag("value");
	Symbol _Member = Symbol.tag("member");
	Symbol _Name = Symbol.tag("name");
	Symbol _Type = Symbol.tag("type");
	Symbol _List = Symbol.tag("list");
}
