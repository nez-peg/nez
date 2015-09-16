package nez.lang.schema;

import java.util.List;

import nez.ast.AbstractTree;

public abstract class Schema {
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

	abstract public Type newTEnum(AbstractTree<?> node);

	abstract public Type newTInteger();

	abstract public Type newTFloat();

	abstract public Type newTString();

	abstract public Type newTAny();

	abstract public Type newSet(String table, List<String> list);

	abstract public Type newPermutation(List<String> required, List<String> implied);

	abstract public Type newUniq(String table, String elementName);

	abstract public Type newOtherAny(String table, List<String> members);
}
