package nez.lang.schema;

import java.util.ArrayList;
import java.util.List;

import nez.ast.Tree;
import nez.lang.GrammarFile;

public abstract class SchemaGrammarGenerator {
	private GrammarFile gfile;
	private List<String> requiredList;
	private List<String> membersList;

	public void addRequired(String name) {
		this.requiredList.add(name);
		this.membersList.add(name);
	}

	public void addMember(String name) {
		this.membersList.add(name);
	}

	public List<String> getMembers() {
		return this.membersList;
	}

	public final void initMemberList() {
		requiredList = new ArrayList<String>();
		membersList = new ArrayList<String>();
	}

	protected final List<String> extractImpliedMembers() {
		List<String> impliedList = new ArrayList<String>();
		for (int i = 0; i < membersList.size(); i++) {
			if (!requiredList.contains(membersList.get(i))) {
				impliedList.add(membersList.get(i));
			}
		}
		return impliedList;
	}

	abstract public void loadPredefinedRules();

	abstract public void newRoot(String structName);

	abstract public Element newElement(String name, Type t);

	abstract public void newStruct(String name, Type t);

	abstract public void newMembers(String structName, Type... types);

	abstract public Type newOption(Type t);

	abstract public Type newRequired(Type t);

	abstract public Type newTObject();

	abstract public Type newTStruct();

	abstract public Type newTArray(Type t);

	abstract public Type newTEnum(Tree<?> node);

	abstract public Type newTInteger();

	abstract public Type newTFloat();

	abstract public Type newTString();

	abstract public Type newTAny();

	abstract public Type newSet(String table);

	abstract public Type newPermutation();

	abstract public Type newUniq(String table, String elementName);

	abstract public Type newOtherAny(String table);
}
