package nez.ext;

import java.io.IOException;

import nez.Grammar;
import nez.Parser;
import nez.Strategy;
import nez.ast.Symbol;
import nez.ast.Tree;
import nez.lang.GrammarFileLoader;
import nez.lang.schema.JSONSchemaGrammarGenerator;
import nez.lang.schema.SchemaGrammarGenerator;
import nez.lang.schema.Type;
import nez.util.ConsoleUtils;

public class Gcelery extends GrammarFileLoader {

	public Gcelery() {
	}

	static Parser celeryParser;
	boolean enableNezExtension;
	private SchemaGrammarGenerator schema;
	private String currentStructName;

	@Override
	public Parser getLoaderParser(String start) {
		if (celeryParser == null) {
			try {
				Strategy option = Strategy.newSafeStrategy();
				Grammar g = GrammarFileLoader.loadGrammar("celery.nez", option);
				celeryParser = g.newParser(option);
				strategy.report();
			} catch (IOException e) {
				ConsoleUtils.exit(1, "unload: " + e.getMessage());
			}
			assert (celeryParser != null);
		}
		this.schema = new JSONSchemaGrammarGenerator(getGrammarFile());
		this.enableNezExtension = !strategy.isEnabled("peg", Strategy.PEG);
		return celeryParser;
	}

	@Override
	public void parse(Tree<?> node) {
		schema.loadPredefinedRules();
		visit("visit", node);
		getGrammarFile().dump();
	}

	public final static Symbol _Name = Symbol.tag("Name");
	public final static Symbol _Type = Symbol.tag("Type");

	public final void visitRoot(Tree<?> node) {
		String rootStructName = node.get(0).getText(_Name, "");
		for (Tree<?> classNode : node) {
			this.visit("visit", classNode);
		}
		schema.newRoot(rootStructName);
	}

	public final void visitStruct(Tree<?> node) {
		currentStructName = node.getText(0, null);
		schema.initMemberList();
		for (Tree<?> memberNode : node) {
			this.visit("visit", memberNode);
		}
		if (enableNezExtension) {
			genStruct(currentStructName);
		} else {
			genStruct_Approximate(currentStructName);
		}
	}

	public final void visitName(Tree<?> node) {

	}

	public final void visitRequired(Tree<?> node) {
		String elementName = node.getText(_Name, "");
		schema.addRequired(elementName);
		schema.newElement(getUniqueName(elementName), schema.newRequired(elementName, toType(node.get(_Type))));
	}

	public final void visitOption(Tree<?> node) {
		String elementName = node.getText(_Name, "");
		schema.addMember(elementName);
		schema.newElement(getUniqueName(elementName), schema.newOption(elementName, toType(node.get(_Type))));
	}

	public final Type toType(Tree<?> node) {
		return (Type) this.visit("to", node);
	}

	public final Type toTObject(Tree<?> node) {
		return schema.newTObject();
	}

	public final Type toTStruct(Tree<?> node) {
		return schema.newTStruct(node.toText());
	}

	public final Type toTAny(Tree<?> node) {
		return schema.newTAny();
	}

	public final Type toTArray(Tree<?> node) {
		return schema.newTArray(toType(node.get(0)));
	}

	public final Type toTEnum(Tree<?> node) {
		String[] candidates = new String[node.size()];
		int index = 0;
		for (Tree<?> subnode : node) {
			candidates[index++] = subnode.toText();
		}
		return schema.newTEnum(candidates);
	}

	public final Type toTInteger(Tree<?> node) {
		return schema.newTInteger();
	}

	public final Type toTFloat(Tree<?> node) {
		return schema.newTFloat();
	}

	public final Type toTString(Tree<?> node) {
		return schema.newTString();
	}

	private final void genStruct(String structName) {
		genMembers(structName);
		schema.newStruct(structName, schema.newSet(structName));
	}

	private final void genMembers(String structName) {
		int membersListSize = schema.getMembers().size();
		int index = 0;
		Type[] alt = new Type[membersListSize + 1];
		for (String elementName : schema.getMembers()) {
			alt[index++] = schema.newUniq(getUniqueName(elementName));
		}
		alt[index] = schema.newOthers();
		schema.newMembers(currentStructName, alt);
	}

	private final void genStruct_Approximate(String structName) {
		schema.newStruct(structName, schema.newPermutation());
	}

	private final String getUniqueName(String localName) {
		return currentStructName + "_" + localName;
	}

}