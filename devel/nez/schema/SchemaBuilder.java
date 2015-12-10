package nez.schema;

import nez.GrammarBuilder;
import nez.lang.Grammar;

public abstract class SchemaBuilder extends GrammarBuilder {

	public SchemaBuilder(Grammar g) {
		super(g);
	}

	public abstract void generateSchema(Class<?> t);

}
