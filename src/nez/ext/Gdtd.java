package nez.ext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nez.Grammar;
import nez.Parser;
import nez.Strategy;
import nez.ast.Symbol;
import nez.ast.Tree;
import nez.lang.GrammarFileLoader;
import nez.lang.schema.Type;
import nez.util.ConsoleUtils;

public class Gdtd extends GrammarFileLoader {

	public Gdtd() {
	}

	static Parser dtdParser;
	boolean enableNezExtension;
	private DTDSchemaGrammarGenerator schema;
	private String currentElementName;

	@Override
	public Parser getLoaderParser(String start) {
		if (dtdParser == null) {
			try {
				Strategy option = Strategy.newSafeStrategy();
				Grammar g = GrammarFileLoader.loadGrammar("xmldtd.nez", option);
				dtdParser = g.newParser(option);
				strategy.report();
			} catch (IOException e) {
				ConsoleUtils.exit(1, "unload: " + e.getMessage());
			}
			assert (dtdParser != null);
		}
		this.schema = new DTDSchemaGrammarGenerator(getGrammarFile());
		this.enableNezExtension = !strategy.isEnabled("peg", Strategy.PEG);
		return dtdParser;
	}

	int currentElementID;
	int attDefCount = 0;
	int entityCount = 0;

	List<String> elementNameList = new ArrayList<String>();
	Map<String, Boolean> containsAttributes = new HashMap<String, Boolean>();
	List<Boolean> containsAttributeList = new ArrayList<Boolean>();
	Map<String, Type> attributeTypeMap = new HashMap<String, Type>();

	@Override
	public void parse(Tree<?> node) {
		schema.loadPredefinedRules();
		visit("visit", node);
		getGrammarFile().dump();
	}

	public final static Symbol _Name = Symbol.tag("name");
	public final static Symbol _Type = Symbol.tag("type");
	public final static Symbol _Member = Symbol.tag("member");
	public final static Symbol _Value = Symbol.tag("value");

	public void visitRoot(Tree<?> node) {
		String rootStructName = node.get(0).getText(_Name, "");
		for (Tree<?> subnode : node) {
			this.visit("visit", subnode);
		}
		for (String name : elementNameList) {
			schema.newStruct(name, hasAttribute(name));
		}
		schema.newRoot(rootStructName);
		schema.newEntityList(entityCount);
	}

	public void visitName(Tree<?> node) {
	}

	public void visitElement(Tree<?> node) {
		currentElementName = node.getText(_Name, "");
		elementNameList.add(currentElementName);
		containsAttributes.put(currentElementName, false);
		schema.newMembers(currentElementName + "_Contents", toType(node.get(_Member)));
	}

	public void visitAttlist(Tree<?> node) {
		currentElementName = node.getText(_Name, "");
		schema.initMemberList();
		containsAttributes.put(currentElementName, true);
		for (Tree<?> subnode : node) {
			this.visit("visit", subnode);
		}
		// generate Complete or Approximate Attribute list
		if (enableNezExtension) {
			genAttributeMembers();
			schema.newAttributeList(currentElementName, schema.newSet(currentElementName + "_Attribute"));
			schema.newSymbols();
		} else {
			schema.newAttributeList(currentElementName, schema.newPermutation());
		}
	}

	private final void genAttributeMembers() {
		int attListSize = schema.getMembers().size();
		int index = 0;
		Type[] alt = new Type[attListSize];
		for (String attributeName : schema.getMembers()) {
			alt[index++] = schema.newAlt(getUniqueName(attributeName));
		}
		schema.newMembers(currentElementName + "_Attribute", alt);
	}

	public void visitREQUIRED(Tree<?> node) {
		String attName = node.getText(_Name, "");
		schema.addRequired(attName);
		schema.newElement(getUniqueName(attName), schema.newUniq(attName, toType(node.get(_Type))));
	}

	public void visitIMPLIED(Tree<?> node) {
		String attName = node.getText(_Name, "");
		schema.addMember(attName);
		schema.newElement(getUniqueName(attName), schema.newUniq(attName, toType(node.get(_Type))));
	}

	public void visitFIXED(Tree<?> node) {
		String attName = node.getText(_Name, "");
		schema.addMember(attName);
		schema.newElement(getUniqueName(attName), schema.newUniq(attName, toType(node.get(_Value))));
	}

	public void visitDefault(Tree<?> node) {
		String attName = node.getText(_Name, "");
		schema.addMember(attName);
		schema.newElement(getUniqueName(attName), schema.newUniq(attName, toType(node.get(_Type))));
	}

	// FIXME
	public void visitEntity(Tree<?> node) {
		schema.newEntity(entityCount++, node.getText(_Name, ""), node.getText(_Value, ""));
	}

	private Type toType(Tree<?> node) {
		return (Type) this.visit("to", node);
	}

	public Type toEmpty(Tree<?> node) {
		return schema.newTEmpty();
	}

	public Type toAny(Tree<?> node) {
		return schema.newTAny();
	}

	public Type toZeroMore(Tree<?> node) {
		return schema.newRZeroMore(toType(node.get(0)));
	}

	public Type toOneMore(Tree<?> node) {
		return schema.newROneMore(toType(node.get(0)));
	}

	public Type toOption(Tree<?> node) {
		return schema.newROption(toType(node.get(0)));
	}

	public Type toChoice(Tree<?> node) {
		Type[] l = new Type[node.size()];
		int count = 0;
		for (Tree<?> subnode : node) {
			l[count++] = toType(subnode);
		}
		return schema.newRChoice(l);
	}

	public Type toSequence(Tree<?> node) {
		Type[] l = new Type[node.size()];
		int count = 0;
		for (Tree<?> subnode : node) {
			l[count++] = toType(subnode);
		}
		return schema.newRSequence(l);
	}

	public Type toCDATA(Tree<?> node) {
		return schema.newAttributeType("CDATA");
	}

	public Type toID(Tree<?> node) {
		return schema.newAttributeType("IDTOKEN");
	}

	public Type toIDREF(Tree<?> node) {
		return schema.newAttributeType("IDTOKEN");
	}

	public Type toIDREFS(Tree<?> node) {
		return schema.newAttributeType("IDTOKENS");
	}

	public Type toENTITY(Tree<?> node) {
		return schema.newAttributeType("entity");
	}

	public Type toENTITIES(Tree<?> node) {
		return schema.newAttributeType("entities");
	}

	public Type toNMTOKEN(Tree<?> node) {
		return schema.newAttributeType("NMTOKEN");
	}

	public Type toNMTOKENS(Tree<?> node) {
		return schema.newAttributeType("NMTOKENS");
	}

	public Type toEnum(Tree<?> node) {
		String[] candidates = new String[node.size()];
		int index = 0;
		for (Tree<?> subnode : node) {
			candidates[index++] = subnode.toText();
		}
		return schema.newTEnum(candidates);
	}

	public Type toElName(Tree<?> node) {
		String elementName = node.toText();
		return schema.newAlt(elementName);
	}

	public Type toData(Tree<?> node) {
		return schema.newAlt("PCDATA");
	}

	public Type toSingleData(Tree<?> node) {
		return schema.newAlt("SINGLE_PCDATA");
	}

	private final boolean hasAttribute(String elementName) {
		return containsAttributes.get(elementName);
	}

	private final String getUniqueName(String localName) {
		return currentElementName + "_" + localName;
	}

}
