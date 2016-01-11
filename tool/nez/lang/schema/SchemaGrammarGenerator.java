package nez.lang.schema;

public interface SchemaGrammarGenerator {
	void loadPredefinedRules();

	void newRoot(String structName);

	Element newElement(String elementName, String structName, Schema t);

	void newStruct(String structName, Schema t);

	void newMembers(String structName);

	void newSymbols();

	Schema newTObject();

	Schema newTStruct(String structName);

	Schema newTArray(Schema t);

	Schema newTEnum(String[] candidates);

	Schema newTInteger();

	Schema newTInteger(int min, int max);

	Schema newTFloat();

	Schema newTFloat(int min, int max);

	Schema newTString();

	Schema newTString(int minLength, int maxLength);

	Schema newTAny();

	Schema newSet(String structName);

	Schema newPermutation();

	Schema newUniq(String elementName, Schema t);

	Schema newAlt(String elementName);

	Schema newOthers();
}
