package nez.lang.schema;

public interface SchemaGrammarGenerator {
	public void loadPredefinedRules();

	public void newRoot(String structName);

	public Element newElement(String elementName, String structName, Schema t);

	public void newStruct(String structName, Schema t);

	public void newMembers(String structName);

	public void newSymbols();

	public Schema newTObject();

	public Schema newTStruct(String structName);

	public Schema newTArray(Schema t);

	public Schema newTEnum(String[] candidates);

	public Schema newTInteger();

	public Schema newTInteger(int min, int max);

	public Schema newTFloat();

	public Schema newTFloat(int min, int max);

	public Schema newTString();

	public Schema newTString(int minLength, int maxLength);

	public Schema newTAny();

	public Schema newSet(String structName);

	public Schema newPermutation();

	public Schema newUniq(String elementName, Schema t);

	public Schema newAlt(String elementName);

	public Schema newOthers();
}
