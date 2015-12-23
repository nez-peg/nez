package nez.lang.schema;

import nez.ast.Tree;
import nez.lang.Grammar;
import nez.parser.ParserStrategy;

public class DTDSchemaConstructor extends SchemaConstructor {
	String elementName = null;
	DTDSchemaGrammarGenerator generator;

	public DTDSchemaConstructor(Grammar grammar, ParserStrategy strategy, DTDSchemaGrammarGenerator generator) {
		super(grammar, strategy);
		this.generator = generator;
		init(DTDSchemaConstructor.class, new Undefined());
	}

	public Schema toSchema(Tree<?> node) {
		return find(node.getTag().toString()).accept(node);
	}

	public final void setElementName(String elementName) {
		this.elementName = elementName;
	}

	public class Undefined implements SchemaTransducer, SchemaSymbol {
		@Override
		public Schema accept(Tree<?> node) {
			undefined(node);
			return null;
		}
	}

	// For DTD attribute declaration
	public final class REQUIRED extends Undefined {
		@Override
		public Schema accept(Tree<?> node) {
			String attName = node.getText(_Name, "");
			Element element = generator.newElement(attName, elementName, generator.newUniq(attName, toSchema(node.get(_Type))));
			generator.addRequired(element);
			return null;
		}
	}

	public final class IMPLIED extends Undefined {
		@Override
		public Schema accept(Tree<?> node) {
			String attName = node.getText(_Name, "");
			Element element = generator.newElement(attName, elementName, generator.newUniq(attName, toSchema(node.get(_Type))));
			element.setOptional(true);
			generator.addElement(element);
			return null;
		}
	}

	public final class FIXED extends Undefined {
		@Override
		public Schema accept(Tree<?> node) {
			String attName = node.getText(_Name, "");
			Element element = generator.newElement(attName, elementName, generator.newUniq(attName, toSchema(node.get(_Value))));
			element.setOptional(true);
			generator.addElement(element);
			return null;
		}
	}

	public final class Default extends Undefined {
		@Override
		public Schema accept(Tree<?> node) {
			String attName = node.getText(_Name, "");
			Element element = generator.newElement(attName, elementName, generator.newUniq(attName, toSchema(node.get(_Type))));
			element.setOptional(true);
			generator.addElement(element);
			return null;
		}
	}

	// the followings are methods that return schema converted from types of DTD
	public final class Empty extends Undefined {
		@Override
		public Schema accept(Tree<?> node) {
			return generator.newTEmpty();
		}
	}

	public final class Any extends Undefined {
		@Override
		public Schema accept(Tree<?> node) {
			return generator.newTAny();
		}
	}

	public final class ZeroMore extends Undefined {
		@Override
		public Schema accept(Tree<?> node) {
			return generator.newRZeroMore(toSchema(node.get(0)));
		}
	}

	public final class OneMore extends Undefined {
		@Override
		public Schema accept(Tree<?> node) {
			return generator.newROneMore(toSchema(node.get(0)));
		}
	}

	public final class Option extends Undefined {
		@Override
		public Schema accept(Tree<?> node) {
			return generator.newROption(toSchema(node.get(0)));
		}
	}

	public final class _Choice extends Undefined {
		@Override
		public Schema accept(Tree<?> node) {
			Schema[] l = new Schema[node.size()];
			int count = 0;
			for (Tree<?> subnode : node) {
				l[count++] = toSchema(subnode);
			}
			return generator.newRChoice(l);
		}
	}

	public final class _Sequence extends Undefined {
		@Override
		public Schema accept(Tree<?> node) {
			Schema[] l = new Schema[node.size()];
			int count = 0;
			for (Tree<?> subnode : node) {
				l[count++] = toSchema(subnode);
			}
			return generator.newRSequence(l);
		}
	}

	public final class CDATA extends Undefined {
		@Override
		public Schema accept(Tree<?> node) {
			return generator.newAttributeType("CDATA");
		}
	}

	public final class ID extends Undefined {
		@Override
		public Schema accept(Tree<?> node) {
			return generator.newAttributeType("IDTOKEN");
		}
	}

	public final class IDREF extends Undefined {
		@Override
		public Schema accept(Tree<?> node) {
			return generator.newAttributeType("IDTOKEN");
		}
	}

	public final class IDREFS extends Undefined {
		@Override
		public Schema accept(Tree<?> node) {
			return generator.newAttributeType("IDTOKENS");
		}
	}

	public final class ENTITY extends Undefined {
		@Override
		public Schema accept(Tree<?> node) {
			return generator.newAttributeType("entity");
		}
	}

	public final class ENTITIES extends Undefined {
		@Override
		public Schema accept(Tree<?> node) {
			return generator.newAttributeType("entities");
		}
	}

	public final class NMTOKEN extends Undefined {
		@Override
		public Schema accept(Tree<?> node) {
			return generator.newAttributeType("NMTOKEN");
		}
	}

	public final class NMTOKENS extends Undefined {
		@Override
		public Schema accept(Tree<?> node) {
			return generator.newAttributeType("NMTOKENS");
		}
	}

	public final class _Enum extends Undefined {
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

	public final class ElName extends Undefined {
		@Override
		public Schema accept(Tree<?> node) {
			String elementName = node.toText();
			return generator.newAlt(elementName);
		}
	}

	public final class Data extends Undefined {
		@Override
		public Schema accept(Tree<?> node) {
			return generator.newAlt("PCDATA");
		}
	}

	public final class SingleData extends Undefined {
		@Override
		public Schema accept(Tree<?> node) {
			return generator.newAttributeType("SINGLE_PCDATA");
		}
	}

}
