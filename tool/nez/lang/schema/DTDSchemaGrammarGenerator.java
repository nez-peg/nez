package nez.lang.schema;

import nez.junks.GrammarFile;
import nez.lang.Expression;

public class DTDSchemaGrammarGenerator extends SchemaGrammarGenerator {

	public DTDSchemaGrammarGenerator(GrammarFile gfile) {
		super(gfile);
	}

	@Override
	public void loadPredefinedRules() {
		new XMLGrammarCombinator(gfile);
	}

	@Override
	public void newRoot(String structName) {
		gfile.addProduction(null, "Root", _NonTerminal(structName));
	}

	@Override
	public Element newElement(String elementName, String structName, Schema t) {
		Expression seq = _Sequence(t.getSchemaExpression(), _S(), _Char('='), _S(), _DQuat(), t.next().getSchemaExpression(), _DQuat(), _S());
		Element element = new Element(elementName, structName, seq, false);
		gfile.addProduction(null, element.getUniqueName(), seq);
		return element;
	}

	public void newMembers(String memberListName, Schema t) {
		gfile.addProduction(null, memberListName, t.getSchemaExpression());
	}

	@Override
	public void newStruct(String structName, Schema t) {
		Expression attOnly = _String("/>");
		Expression[] content = { _Char('>'), _S(), _NonTerminal("%s_Contents", structName), _S(), _String("</%s>", structName) };
		Expression endChoice = _Choice(_Sequence(attOnly), _Sequence(content));
		Expression[] l = { _String("<%s", structName), _S(), t.getSchemaExpression(), endChoice, _S() };
		gfile.addProduction(null, structName, _Sequence(l));
	}

	public final void newStruct(String structName, boolean hasAttribute) {
		Expression inner = _Empty();
		if (hasAttribute) {
			inner = _Sequence(_NonTerminal("%s_AttributeList", structName), _S());
		}
		newStruct(structName, new Schema(inner));
	}

	public void newEntity(int id, String name, String value) {
		String entity = String.format("Entity_%s", id);
		gfile.addProduction(null, entity, _String(value));
	}

	@Override
	public Schema newTObject() {
		return null;
	}

	@Override
	public Schema newTStruct(String structName) {
		return new Schema(_NonTerminal(structName));
	}

	@Override
	public Schema newTArray(Schema t) {
		return null;
	}

	@Override
	public Schema newTEnum(String[] candidates) {
		Expression[] l = new Expression[candidates.length];
		int index = 0;
		for (String candidate : candidates) {
			l[index++] = _String(candidate);
		}
		return new Schema(_Choice(l));
	}

	public Schema newTEmpty() {
		return new Schema(_NonTerminal("EMPTY"));
	}

	public Schema newAttributeType(String type) {
		return new Schema(_NonTerminal(type));
	}

	@Override
	public Schema newOthers() {
		return new Schema(_Failure());
	}

	public Schema newRZeroMore(Schema type) {
		return new Schema(_ZeroMore(type.getSchemaExpression()));
	}

	public Schema newROneMore(Schema type) {
		return new Schema(_OneMore(type.getSchemaExpression()));
	}

	public Schema newROption(Schema type) {
		return new Schema(_Option(type.getSchemaExpression()));
	}

	public Schema newRChoice(Schema... l) {
		Expression[] seq = new Expression[l.length];
		int index = 0;
		for (Schema type : l) {
			seq[index++] = type.getSchemaExpression();
		}
		return new Schema(_Choice(seq));
	}

	public Schema newRSequence(Schema... l) {
		Expression[] seq = new Expression[l.length];
		int index = 0;
		for (Schema type : l) {
			seq[index++] = type.getSchemaExpression();
		}
		return new Schema(_Sequence(seq));
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
	protected Schema newCompletePerm() {
		int listLength = getRequiredElementList().size();
		if (listLength == 1) {
			return new Schema(_Sequence(_NonTerminal(getRequiredElementList().get(0).getUniqueName()), _NonTerminal("ENDTAG")));
		} else {
			PermutationGenerator permGen = new PermutationGenerator(listLength);
			int[][] permedList = permGen.getPermList();
			Expression[] choiceList = new Expression[permedList.length];
			int choiceCount = 0;
			for (int[] targetLine : permedList) {
				Expression[] seqList = new Expression[listLength + 1];
				for (int index = 0; index < targetLine.length; index++) {
					seqList[index] = _NonTerminal(getRequiredElementList().get(targetLine[index]).getUniqueName());
				}
				seqList[listLength] = _NonTerminal("ENDTAG");
				choiceList[choiceCount++] = _Sequence(seqList);
			}
			return new Schema(_Choice(choiceList));
		}
	}

	@Override
	protected Schema newAproximatePerm() {
		int listLength = getRequiredElementList().size();
		if (listLength == 0) {
			return new Schema(_NonTerminal("%s_implied", getTableName()));
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
					seqList[seqCount++] = _NonTerminal(getRequiredElementList().get(targetLine[index]).getUniqueName());
					seqList[seqCount++] = _ZeroMore(impliedChoiceRule);
				}
				seqList[seqCount] = _NonTerminal("ENDTAG");
				choiceList[choiceCount++] = _Sequence(seqList);
			}
			return new Schema(_Choice(choiceList));
		}
	}

}
