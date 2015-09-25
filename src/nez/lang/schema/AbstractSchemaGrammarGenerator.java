package nez.lang.schema;

public abstract class AbstractSchemaGrammarGenerator {
	abstract public void loadPredefinedRules();

	abstract public void newRoot(String structName);

	abstract public void newElement(String elementName, Type t);

	abstract public void newStruct(String structName, Type t);

	abstract public void newMembers(String structName, Type... types);

	abstract public Type newRequired(String elementName, Type t);

	abstract public Type newOption(String elementName, Type t);

	abstract public Type newTObject();

	abstract public Type newTStruct(String structName);

	abstract public Type newTArray(Type t);

	abstract public Type newTEnum(String[] candidates);

	abstract public Type newTInteger();

	abstract public Type newTFloat();

	abstract public Type newTString();

	abstract public Type newTAny();

	abstract public Type newSet(String structName);

	abstract public Type newPermutation();

	abstract public Type newUniq(String elementName);

	abstract public Type newOthers();
}
