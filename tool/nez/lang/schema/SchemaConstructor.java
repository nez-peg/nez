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
	public Schema accept(Tree<?> node);
}

interface SchemaSymbol {
	static final Symbol _Key = Symbol.tag("key");
	static final Symbol _Value = Symbol.tag("value");
	static final Symbol _Member = Symbol.tag("member");
	static final Symbol _Name = Symbol.tag("name");
	static final Symbol _Type = Symbol.tag("type");
	static final Symbol _List = Symbol.tag("list");
}
