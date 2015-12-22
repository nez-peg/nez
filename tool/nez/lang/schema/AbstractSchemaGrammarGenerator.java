package nez.lang.schema;

public abstract class AbstractSchemaGrammarGenerator {
	abstract public void loadPredefinedRules();

	abstract public void newRoot(String structName);

	abstract public Element newElement(String elementName, String structName, Schema t);

	abstract public void newStruct(String structName, Schema t);

	abstract public void newMembers(String structName);

	abstract public void newSymbols();

	abstract public Schema newTObject();

	abstract public Schema newTStruct(String structName);

	abstract public Schema newTArray(Schema t);

	abstract public Schema newTEnum(String[] candidates);

	abstract public Schema newTInteger();

	abstract public Schema newTInteger(int min, int max);

	abstract public Schema newTFloat();

	abstract public Schema newTFloat(int min, int max);

	abstract public Schema newTString();

	abstract public Schema newTString(int minLength, int maxLength);

	abstract public Schema newTAny();

	abstract public Schema newSet(String structName);

	abstract public Schema newPermutation();

	abstract public Schema newUniq(String elementName, Schema t);

	abstract public Schema newAlt(String elementName);

	abstract public Schema newOthers();
}
