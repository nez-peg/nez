package nez.lang.schema;

import java.util.List;

import nez.ast.AbstractTree;
import nez.lang.GrammarFile;

public class JSONSchemaGrammarGenerator extends SchemaGrammarGenerator {

	private GrammarFile gfile;

	public JSONSchemaGrammarGenerator(GrammarFile gfile) {
		this.gfile = gfile;
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
	public Element newElement(String name, Type t) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void newStruct(String name, Type t) {
		// TODO Auto-generated method stub

	}

	@Override
	public void newMembers(String structName, Type... types) {
		// TODO Auto-generated method stub

	}

	@Override
	public Type newOption(Type t) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Type newRequired(Type t) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Type newTObject() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Type newTStruct() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Type newTArray(Type t) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Type newTEnum(AbstractTree<?> node) {
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
	public Type newSet(String table, List<String> list) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Type newPermutation(List<String> required, List<String> implied) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Type newUniq(String table, String elementName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Type newOtherAny(String table, List<String> members) {
		// TODO Auto-generated method stub
		return null;
	}

}
