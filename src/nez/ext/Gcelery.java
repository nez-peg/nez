package nez.ext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import nez.Grammar;
import nez.NezOption;
import nez.Parser;
import nez.ast.AbstractTree;
import nez.ast.SymbolId;
import nez.lang.GrammarFileLoader;
import nez.lang.schema.JSONSchema;
import nez.lang.schema.Schema;
import nez.lang.schema.Type;
import nez.util.ConsoleUtils;

public class Gcelery extends GrammarFileLoader {

	public Gcelery() {
	}

	static Parser celeryParser;
	boolean enableNezExtension;
	private Schema schema;

	@Override
	public Parser getLoaderGrammar() {
		if (celeryParser == null) {
			try {
				NezOption option = NezOption.newSafeOption();
				Grammar g = GrammarFileLoader.loadGrammar("celery.nez", option, null);
				celeryParser = g.newParser(option, repo);
				if (repo != null) {
					repo.report(option);
				}
			} catch (IOException e) {
				ConsoleUtils.exit(1, "unload: " + e.getMessage());
			}
			assert (celeryParser != null);
		}
		this.schema = new JSONSchema(getGrammarFile());
		this.enableNezExtension = !option.disabledNezExtension;
		return celeryParser;
	}

	@Override
	public void parse(AbstractTree<?> node) {
		schema.loadPredefinedRules();
		visit("visit", node);
	}

	private List<String> requiredList;
	private List<String> membersList;

	public final static SymbolId _Name = SymbolId.tag("Name");
	public final static SymbolId _Type = SymbolId.tag("Type");

	public final void visitRoot(AbstractTree<?> node) {
		String rootStructName = node.get(0).getText(_Name, "");
		for (AbstractTree<?> classNode : node) {
			this.visit("visit", classNode);
		}
		schema.newRoot(rootStructName);
	}

	public final void visitStruct(AbstractTree<?> node) {
		String structName = node.getText(0, null);
		initMemberList();
		for (AbstractTree<?> memberNode : node) {
			this.visit("visit", memberNode);
		}
		if (enableNezExtension) {
			genStruct(structName);
		} else {
			genStruct_Approximate(structName);
		}
	}

	public final void visitRequired(AbstractTree<?> node) {
		String elementName = node.getText(_Name, "");
		requiredList.add(elementName);
		membersList.add(elementName);
		schema.newElement(elementName, schema.newRequired(toType(node.get(_Type))));
	}

	public final void visitOption(AbstractTree<?> node) {
		String elementName = node.getText(_Name, "");
		membersList.add(elementName);
		schema.newElement(elementName, schema.newOption(toType(node.get(_Type))));
	}

	public final Type toType(AbstractTree<?> node) {
		return (Type) this.visit("to", node);
	}

	public final Type toTObject(AbstractTree<?> node) {
		return schema.newTObject();
	}

	public final Type toTStruct(AbstractTree<?> node) {
		return schema.newTStruct();
	}

	public final Type toTAny(AbstractTree<?> node) {
		return schema.newTAny();
	}

	public final Type toTArray(AbstractTree<?> node) {
		return schema.newTArray(toType(node.get(0)));
	}

	public final Type toTEnum(AbstractTree<?> node) {
		return schema.newTEnum(node);
	}

	public final Type toTInteger(AbstractTree<?> node) {
		return schema.newTInteger();
	}

	public final Type toTFloat(AbstractTree<?> node) {
		return schema.newTFloat();
	}

	private final void genStruct(String structName) {
		genMembers(structName);
		schema.newStruct(structName, schema.newSet(structName, requiredList));
	}

	private final void genMembers(String structName) {
		int membersListSize = membersList.size();
		int count = 0;
		Type[] alt = new Type[membersListSize + 1];
		for (String elementName : membersList) {
			alt[count++] = schema.newUniq(structName, elementName);
		}
		alt[count] = schema.newOtherAny(structName, membersList);
		schema.newMembers(structName, alt);
	}

	private final void genStruct_Approximate(String structName) {
		schema.newStruct(structName, schema.newPermutation(requiredList, extractImpliedMembers()));
	}

	private final void initMemberList() {
		requiredList = new ArrayList<String>();
		membersList = new ArrayList<String>();
	}

	private final List<String> extractImpliedMembers() {
		List<String> impliedList = new ArrayList<String>();
		for (int i = 0; i < membersList.size(); i++) {
			if (!requiredList.contains(membersList.get(i))) {
				impliedList.add(membersList.get(i));
			}
		}
		return impliedList;
	}

}