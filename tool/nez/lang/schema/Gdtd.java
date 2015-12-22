package nez.lang.schema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nez.ast.Symbol;
import nez.ast.Tree;
import nez.junks.GrammarFileLoader;
import nez.parser.Parser;
import nez.util.ConsoleUtils;

public class Gdtd extends GrammarFileLoader {

	public Gdtd() {
		init(Gdtd.class, new Undefined());
	}

	public class Undefined extends DefaultVisitor {
		@Override
		public void accept(Tree<?> node) {
			ConsoleUtils.println(node.formatSourceMessage("error", "unsupproted in DTDConverter #" + node));
		}

		@Override
		public Schema toSchema(Tree<?> node) {
			ConsoleUtils.println(node.formatSourceMessage("error", "unsupproted in DTDConverter #" + node));
			return null;
		}
	}

	static Parser dtdParser;
	boolean enableNezExtension;
	private DTDSchemaGrammarGenerator schema;
	private String currentElementName;

	// @Override
	// public Parser getLoaderParser(String start) {
	// if (dtdParser == null) {
	// try {
	// Strategy option = Strategy.newSafeStrategy();
	// Grammar g = GrammarFileLoader.loadGrammar("xmldtd.nez", option);
	// dtdParser = g.newParser(option);
	// strategy.report();
	// } catch (IOException e) {
	// ConsoleUtils.exit(1, "unload: " + e.getMessage());
	// }
	// assert (dtdParser != null);
	// }
	// this.schema = new DTDSchemaGrammarGenerator(getGrammarFile());
	// this.enableNezExtension = !strategy.isEnabled("peg", Strategy.PEG);
	// return dtdParser;
	// }

	int currentElementID;
	int attDefCount = 0;
	int entityCount = 0;

	List<String> elementNameList = new ArrayList<String>();
	Map<String, Boolean> containsAttributes = new HashMap<String, Boolean>();
	Map<String, Schema> attributeTypeMap = new HashMap<String, Schema>();

	private final void visit(Tree<?> node) {
		find(node.getTag().toString()).accept(node);
	}

	private final Schema toType(Tree<?> node) {
		return find(node.getTag().toString()).toSchema(node);
	}

	@Override
	public void parse(Tree<?> node) {
		schema.loadPredefinedRules();
		visit(node);
		// getGrammarFile().dump();
	}

	public final static Symbol _Name = Symbol.tag("name");
	public final static Symbol _Type = Symbol.tag("type");
	public final static Symbol _Member = Symbol.tag("member");
	public final static Symbol _Value = Symbol.tag("value");

	public final class Root extends Undefined {
		@Override
		public void accept(Tree<?> node) {
			String rootStructName = node.get(0).getText(_Name, "");
			for (Tree<?> subnode : node) {
				visit(subnode);
			}
			for (String name : elementNameList) {
				schema.newStruct(name, hasAttribute(name));
			}
			schema.newRoot(rootStructName);
			schema.newEntityList(entityCount);
		}
	}

	public final class Name extends Undefined {
		@Override
		public void accept(Tree<?> node) {
		}
	}

	public final class _Element extends Undefined {
		@Override
		public void accept(Tree<?> node) {
			currentElementName = node.getText(_Name, "");
			elementNameList.add(currentElementName);
			containsAttributes.put(currentElementName, false);
			schema.newMembers(String.format("%s_Contents", currentElementName), toType(node.get(_Member)));
		}
	}

	public final class Attlist extends Undefined {
		@Override
		public void accept(Tree<?> node) {
			currentElementName = node.getText(_Name, "");
			schema.initMemberList();
			containsAttributes.put(currentElementName, true);
			for (Tree<?> subnode : node) {
				visit(subnode);
			}
			// generate Complete or Approximate Attribute list
			if (enableNezExtension) {
				genAttributeMembers();
				schema.newMembers(String.format("%s_AttributeList", currentElementName), schema.newSet(String.format("%s_Attribute", currentElementName)));
				schema.newSymbols();
			} else {
				schema.newMembers(String.format("%s_AttributeList", currentElementName), schema.newPermutation());
			}
		}
	}

	private final void genAttributeMembers() {
		schema.newMembers(String.format("%s_Attribute", currentElementName));
	}

	public final class REQUIRED extends Undefined {
		@Override
		public void accept(Tree<?> node) {
			String attName = node.getText(_Name, "");
			Element element = schema.newElement(attName, currentElementName, schema.newUniq(attName, toType(node.get(_Type))));
			schema.addRequired(element);
		}
	}

	public final class IMPLIED extends Undefined {
		@Override
		public void accept(Tree<?> node) {
			String attName = node.getText(_Name, "");
			Element element = schema.newElement(attName, currentElementName, schema.newUniq(attName, toType(node.get(_Type))));
			element.setOptional(true);
			schema.addElement(element);
		}
	}

	public final class FIXED extends Undefined {
		@Override
		public void accept(Tree<?> node) {
			String attName = node.getText(_Name, "");
			Element element = schema.newElement(attName, currentElementName, schema.newUniq(attName, toType(node.get(_Value))));
			element.setOptional(true);
			schema.addElement(element);
		}
	}

	public final class Default extends Undefined {
		@Override
		public void accept(Tree<?> node) {
			String attName = node.getText(_Name, "");
			Element element = schema.newElement(attName, currentElementName, schema.newUniq(attName, toType(node.get(_Type))));
			element.setOptional(true);
			schema.addElement(element);
		}
	}

	public final class _Entity extends Undefined {
		@Override
		public void accept(Tree<?> node) {
			schema.newEntity(entityCount++, node.getText(_Name, ""), node.getText(_Value, ""));
		}
	}

	public final class Empty extends Undefined {
		@Override
		public Schema toSchema(Tree<?> node) {
			return schema.newTEmpty();
		}
	}

	public final class Any extends Undefined {
		@Override
		public Schema toSchema(Tree<?> node) {
			return schema.newTAny();
		}
	}

	public final class ZeroMore extends Undefined {
		@Override
		public Schema toSchema(Tree<?> node) {
			return schema.newRZeroMore(toType(node.get(0)));
		}
	}

	public final class OneMore extends Undefined {
		@Override
		public Schema toSchema(Tree<?> node) {
			return schema.newROneMore(toType(node.get(0)));
		}
	}

	public final class Option extends Undefined {
		@Override
		public Schema toSchema(Tree<?> node) {
			return schema.newROption(toType(node.get(0)));
		}
	}

	public final class _Choice extends Undefined {
		@Override
		public Schema toSchema(Tree<?> node) {
			Schema[] l = new Schema[node.size()];
			int count = 0;
			for (Tree<?> subnode : node) {
				l[count++] = toType(subnode);
			}
			return schema.newRChoice(l);
		}
	}

	public final class _Sequence extends Undefined {
		@Override
		public Schema toSchema(Tree<?> node) {
			Schema[] l = new Schema[node.size()];
			int count = 0;
			for (Tree<?> subnode : node) {
				l[count++] = toType(subnode);
			}
			return schema.newRSequence(l);
		}
	}

	public final class CDATA extends Undefined {
		@Override
		public Schema toSchema(Tree<?> node) {
			return schema.newAttributeType("CDATA");
		}
	}

	public final class ID extends Undefined {
		@Override
		public Schema toSchema(Tree<?> node) {
			return schema.newAttributeType("IDTOKEN");
		}
	}

	public final class IDREF extends Undefined {
		@Override
		public Schema toSchema(Tree<?> node) {
			return schema.newAttributeType("IDTOKEN");
		}
	}

	public final class IDREFS extends Undefined {
		@Override
		public Schema toSchema(Tree<?> node) {
			return schema.newAttributeType("IDTOKENS");
		}
	}

	public final class ENTITY extends Undefined {
		@Override
		public Schema toSchema(Tree<?> node) {
			return schema.newAttributeType("entity");
		}
	}

	public final class ENTITIES extends Undefined {
		@Override
		public Schema toSchema(Tree<?> node) {
			return schema.newAttributeType("entities");
		}
	}

	public final class NMTOKEN extends Undefined {
		@Override
		public Schema toSchema(Tree<?> node) {
			return schema.newAttributeType("NMTOKEN");
		}
	}

	public final class NMTOKENS extends Undefined {
		@Override
		public Schema toSchema(Tree<?> node) {
			return schema.newAttributeType("NMTOKENS");
		}
	}

	public final class _Enum extends Undefined {
		@Override
		public Schema toSchema(Tree<?> node) {
			String[] candidates = new String[node.size()];
			int index = 0;
			for (Tree<?> subnode : node) {
				candidates[index++] = subnode.toText();
			}
			return schema.newTEnum(candidates);
		}
	}

	public final class ElName extends Undefined {
		@Override
		public Schema toSchema(Tree<?> node) {
			String elementName = node.toText();
			return schema.newAlt(elementName);
		}
	}

	public final class Data extends Undefined {
		@Override
		public Schema toSchema(Tree<?> node) {
			return schema.newAlt("PCDATA");
		}
	}

	public final class SingleData extends Undefined {
		@Override
		public Schema toSchema(Tree<?> node) {
			return schema.newAttributeType("SINGLE_PCDATA");
		}
	}

	private final boolean hasAttribute(String elementName) {
		return containsAttributes.get(elementName);
	}

	private final String getUniqueName(String localName) {
		return String.format("%s_%s", currentElementName, localName);
	}

	@Override
	public Parser getLoaderParser(String start) {
		// TODO Auto-generated method stub
		return null;
	}
}
