package nez.schema;

import nez.Grammar;
import nez.GrammarBuilder;

public abstract class SchemaBuilder extends GrammarBuilder {

	public SchemaBuilder(Grammar g) {
		super(g);
	}

	public abstract void generateSchema(Class<?> t);

}
