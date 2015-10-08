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
		Expression[] l = { t.getTypeExpression(), _S(), _Char('='), _S(), _DQuat(), t.next().getTypeExpression(), _DQuat(), _S() };
		gfile.addProduction(null, elementName, _Sequence(l));
	}

	public void newAttributeList(String name, Type t) {
		gfile.addProduction(null, String.format("%s_AttributeList", name), t.getTypeExpression());
	}

	@Override
	public void newStruct(String structName, Type t) {
		Expression attOnly = _String("/>");
		Expression[] content = { _Char('>'), _S(), _NonTerminal("%s_Contents", structName), _S(), _String("</%s>", structName) };
		Expression endChoice = _Choice(_Sequence(attOnly), _Sequence(content));
		Expression[] l = { _String("<%s", structName), _S(), t.getTypeExpression(), endChoice, _S() };
		gfile.addProduction(null, structName, _Sequence(l));
	}

	public final void newStruct(String structName, boolean hasAttribute) {
		Expression inner = _Empty();
		if (hasAttribute) {
			inner = _Sequence(_NonTerminal("%s_AttributeList", structName), _S());
		}
		newStruct(structName, new Type(inner));
	}

	public void newEntity(int id, String name, String value) {
		String entity = String.format("Entity_%s", id);
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
		return new Type(_OneMore(type.getTypeExpression()));
	}

	public Type newROption(Type type) {
		return new Type(_Option(type.getTypeExpression()));
	}

	public Type newRChoice(Type... l) {
		Expression[] seq = new Expression[l.length];
		int index = 0;
		for (Type type : l) {
			seq[index++] = type.getTypeExpression();
		}
		return new Type(_Choice(seq));
	}

	public Type newRSequence(Type... l) {
		Expression[] seq = new Expression[l.length];
		int index = 0;
		for (Type type : l) {
			seq[index++] = type.getTypeExpression();
		}
		return new Type(_Sequence(seq));
	}

	public void newEntityList(int entityCount) {
		if (entityCount == 0) {
			gfile.addProduction(null, "Entity", _Failure());
		} else {
			Expression[] l = new Expression[entityCount];
			for (int i = 0; i < entityCount; i++) {
				l[i] = _NonTerminal("Entity_%s", entityCount);
			}
			gfile.addProduction(null, "Entity", _Choice(l));
		}
	}

	@Override
	protected Type newCompletePerm() {
		int listLength = getRequiredList().size();
		if (listLength == 1) {
			return new Type(_Sequence(_NonTerminal(getRequiredList().get(0)), _NonTerminal("ENDTAG")));
		} else {
			PermutationGenerator permGen = new PermutationGenerator(listLength);
			int[][] permedList = permGen.getPermList();
			Expression[] choiceList = new Expression[permedList.length];
			int choiceCount = 0;
			for (int[] targetLine : permedList) {
				Expression[] seqList = new Expression[listLength + 1];
				for (int index = 0; index < targetLine.length; index++) {
					seqList[index] = _NonTerminal(getRequiredList().get(targetLine[index]));
				}
				seqList[listLength] = _NonTerminal("ENDTAG");
				choiceList[choiceCount++] = _Sequence(seqList);
			}
			return new Type(_Choice(choiceList));
		}
	}

	@Override
	protected Type newAproximatePerm() {
		int listLength = getRequiredList().size();
		if (listLength == 0) {
			return new Type(_NonTerminal("%s_implied", getTableName()));
		} else {
			PermutationGenerator permGen = new PermutationGenerator(listLength);
			int[][] permutedList = permGen.getPermList();
			Expression[] choiceList = new Expression[permutedList.length];
			Expression impliedChoiceRule = _NonTerminal("%s_implied", getTableName());
			int choiceCount = 0;
			for (int[] targetLine : permutedList) {
				int seqCount = 0;
				Expression[] seqList = new Expression[listLength * 2 + 1];
				seqList[seqCount++] = _ZeroMore(impliedChoiceRule);
				for (int index = 0; index < targetLine.length; index++) {
					seqList[seqCount++] = _NonTerminal(getRequiredList().get(targetLine[index]));
					seqList[seqCount++] = _ZeroMore(impliedChoiceRule);
				}
				seqList[seqCount] = _NonTerminal("ENDTAG");
				choiceList[choiceCount++] = _Sequence(seqList);
			}
			return new Type(_Choice(choiceList));
		}
	}
}
