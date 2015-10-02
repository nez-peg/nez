package nez.lang.schema;

import nez.lang.GrammarFile;

public class XMLSchemaGrammarGenerator extends SchemaGrammarGenerator {

	public XMLSchemaGrammarGenerator(GrammarFile gfile) {
		super(gfile);
	}

	@Override
	public void loadPredefinedRules() {
		// TODO Auto-generated method stub

	}

	@Override
	public void newRoot(String structName) {
		// TODO Auto-generated method stub

	}

	@Override
	public void newElement(String elementName, Type t) {
		// TODO Auto-generated method stub

	}

	@Override
	public void newStruct(String structName, Type t) {
		// TODO Auto-generated method stub

	}

	@Override
	public void newMembers(String structName, Type... members) {
		// TODO Auto-generated method stub

	}

	@Override
	public void newUniqNames() {
		// TODO Auto-generated method stub

	}

	@Override
	public Type newRequired(String elementName, Type t) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Type newOption(String elementName, Type t) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Type newTObject() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Type newTStruct(String structName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Type newTArray(Type t) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Type newTEnum(String[] candidates) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Type newTInteger() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Type newTFloat() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Type newTString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Type newTAny() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Type newSet(String structName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Type newPermutation() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Type newUniq(String elementName, Type t) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Type newAlt(String elementName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Type newOthers() {
		// TODO Auto-generated method stub
		return null;
	}

}
