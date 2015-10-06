package nez.lang.schema;

import nez.lang.Expression;
import nez.lang.GrammarFile;

public class DTDSchemaGrammarGenerator extends SchemaGrammarGenerator {

	public DTDSchemaGrammarGenerator(GrammarFile gfile) {
		super(gfile);
	}

	@Override
	public void loadPredefinedRules() {
		XMLPredefinedGrammar pre = new XMLPredefinedGrammar(gfile);
		pre.define();
	}

	@Override
	public void newRoot(String structName) {
		gfile.addProduction(null, "Root", _NonTerminal(structName));
	}

	@Override
	public void newElement(String elementName, Type t) {
		Expression[] l = { _String(elementName), _S(), _Char('='), _S(), _DQuat(), t.getTypeExpression(), _DQuat(), _S() };
		gfile.addProduction(null, elementName, _Sequence(l));
	}

	public void newAttributeList(String name, Type t) {
		gfile.addProduction(null, name + "_Attribute", t.getTypeExpression());
	}

	@Override
	public void newStruct(String structName, Type t) {
		Expression attOnly = _String("/>");
		Expression[] content = { _Char('>'), _S(), _ZeroMore(_NonTerminal(structName + "_SMembers")) };
		Expression endChoice = _Choice(_Sequence(attOnly), _Sequence(content));
		Expression[] l = { _String("<" + structName), _S(), t.getTypeExpression(), _S(), endChoice };
		gfile.addProduction(null, structName, _Sequence(l));
	}

	public final void newStruct(String structName, boolean hasAttribute) {
		Expression inner = _Empty();
		if (hasAttribute) {
			inner = _NonTerminal(structName + "_Attribute");
		}
		newStruct(structName, new Type(inner));
	}

	public void newEntity(int id, String name, String value) {
		String entity = "Entity_" + id;
		gfile.addProduction(null, entity, _String(value));
	}

	@Override
	public Type newTObject() {
		return null;
	}

	@Override
	public Type newTStruct(String structName) {
		return new Type(_NonTerminal(structName));
	}

	@Override
	public Type newTArray(Type t) {
		return null;
	}

	@Override
	public Type newTEnum(String[] candidates) {
		Expression[] l = new Expression[candidates.length];
		int index = 0;
		for (String candidate : candidates) {
			l[index++] = _String(candidate);
		}
		return new Type(_Choice(l));
	}

	public Type newTEmpty() {
		return new Type(_NonTerminal("EMPTY"));
	}

	public Type newAttributeType(String type) {
		return new Type(_NonTerminal(type));
	}

	@Override
	public Type newOthers() {
		return new Type(_Empty());
	}

	public Type newRZeroMore(Type type) {
		return new Type(_ZeroMore(type.getTypeExpression()));
	}

	public Type newROneMore(Type type) {
		// TODO Auto-generated method stub
		return new Type(_OneMore(type.getTypeExpression()));
	}

	public Type newROption(Type type) {
		return new Type(_Option(type.getTypeExpression()));
	}

	public Type newRChoice(Type... l) {
		Expression[] seq = new Expression[l.length];
		int index = 0;
		for (Type type : l) {
			seq[index] = type.getTypeExpression();
		}
		return new Type(_Choice(seq));
	}

	public Type newRSequence(Type... l) {
		Expression[] seq = new Expression[l.length];
		int index = 0;
		for (Type type : l) {
			seq[index] = type.getTypeExpression();
		}
		return new Type(_Sequence(seq));
	}

	public void newEntityList(int entityCount) {
		if (entityCount == 0) {
			gfile.addProduction(null, "Entity", _Failure());
		} else {
			Expression[] l = new Expression[entityCount];
			for (int i = 0; i < entityCount; i++) {
				l[i] = _NonTerminal("Entity_" + entityCount);
			}
			gfile.addProduction(null, "Entity", _Choice(l));
		}

	}

}
