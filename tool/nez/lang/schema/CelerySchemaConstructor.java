package nez.lang.schema;

import nez.ast.Tree;
import nez.lang.Grammar;
import nez.parser.ParserStrategy;

public class CelerySchemaConstructor extends SchemaConstructor {
	String structName = null;
	JSONSchemaGrammarGenerator generator;

	public CelerySchemaConstructor(Grammar grammar, ParserStrategy strategy, JSONSchemaGrammarGenerator generator) {
		super(grammar, strategy);
		this.generator = generator;
		init(CelerySchemaConstructor.class, new Undefined());
	}

	public Schema toSchema(Tree<?> node) {
		return find(node.getTag().toString()).accept(node);
	}

	public void setStructName(String structName) {
		this.structName = structName;
	}

	public class Undefined implements SchemaTransducer, SchemaSymbol {
		@Override
		public Schema accept(Tree<?> node) {
			undefined(node);
			return null;
		}
	}

	public final class Required extends Undefined {
		@Override
		public Schema accept(Tree<?> node) {
			String elementName = node.getText(_Name, "");
			Element element = generator.newElement(elementName, structName, generator.newUniq(elementName, toSchema(node.get(_Type))));
			generator.addRequired(element);
			return null;
		}
	}

	public final class Option extends Undefined {
		@Override
		public Schema accept(Tree<?> node) {
			String elementName = node.getText(_Name, "");
			Element element = generator.newElement(elementName, structName, generator.newUniq(elementName, toSchema(node.get(_Type))));
			element.setOptional(true);
			generator.addElement(element);
			return null;
		}
	}

	public final class TObject extends Undefined {
		@Override
		public Schema accept(Tree<?> node) {
			// if (node.has(_Range)) {
			// int min = Integer.parseInt(node.getText(_Min, ""));
			// int max = Integer.parseInt(node.getText(_Max, ""));
			// // return schema.newTObject(min, max);
			// }
			return generator.newTObject();
		}
	}

	public final class TStruct extends Undefined {
		@Override
		public Schema accept(Tree<?> node) {
			return generator.newTStruct(node.toText());
		}
	}

	public final class TAny extends Undefined {
		@Override
		public Schema accept(Tree<?> node) {
			return generator.newTAny();
		}
	}

	public final class TArray extends Undefined {
		@Override
		public Schema accept(Tree<?> node) {
			// if (node.has(_Range)) {
			// int min = Integer.parseInt(node.getText(_Min, ""));
			// int max = Integer.parseInt(node.getText(_Max, ""));
			// // return schema.newTArray(toType(node), min, max);
			// }
			return generator.newTArray(toSchema(node.get(0)));
		}
	}

	public final class TEnum extends Undefined {
		@Override
		public Schema accept(Tree<?> node) {
			String[] candidates = new String[node.size()];
			int index = 0;
			for (Tree<?> subnode : node) {
				candidates[index++] = subnode.toText();
			}
			return generator.newTEnum(candidates);
		}
	}

	public final class TInteger extends Undefined {
		@Override
		public Schema accept(Tree<?> node) {
			// if (node.has(_Range)) {
			// int min = Integer.parseInt(node.getText(_Min, ""));
			// int max = Integer.parseInt(node.getText(_Max, ""));
			// // return schema.newTInteger(min, max);
			// }
			return generator.newTInteger();
		}
	}

	public final class TFloat extends Undefined {
		@Override
		public Schema accept(Tree<?> node) {
			// if (node.has(_Range)) {
			// float min = Float.parseFloat(node.getText(_Min, ""));
			// float max = Float.parseFloat(node.getText(_Max, ""));
			// // return schema.newTFloat(min, max);
			// }
			return generator.newTFloat();
		}
	}

	public final class TString extends Undefined {
		@Override
		public Schema accept(Tree<?> node) {
			// if (node.has(_Length)) {
			// int min = Integer.parseInt(node.getText(_Min, ""));
			// int max = Integer.parseInt(node.getText(_Max, ""));
			// // return schema.newTString(min, max);
			// }
			return generator.newTString();
		}
	}
}
