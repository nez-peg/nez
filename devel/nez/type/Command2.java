package nez.type;

import java.io.IOException;

import nez.lang.Grammar;

public class Command2 extends nez.main.Command {
	@Override
	public void exec() throws IOException {
		/* Setting requird options */
		strategy.Optimization = false;
		Grammar grammar = this.newGrammar();
		SchemaTransformer conv = new SchemaTransformer();
		Schema schema = new Schema();
		schema.newType("#String");
		schema.newType("#Number");
		Schema.ObjectType topType = schema.newType("#Person", "name", "#String", "age", "#String");
		System.out.println(topType);
		grammar = conv.transform(grammar.getStartProduction(), schema, topType);
		grammar.dump();
	}
}
