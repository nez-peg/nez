package nez.schema;

import nez.lang.Grammar;
import nez.lang.GrammarBuilder;

public abstract class SchemaBuilder extends GrammarBuilder {

	public SchemaBuilder(Grammar g) {
		super(g);
	}

	public abstract void generateSchema(Class<?> t);

}
