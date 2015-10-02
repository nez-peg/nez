package nez.lang.schema;

import java.util.List;

import nez.lang.Expression;
import nez.lang.GrammarFile;
import nez.lang.expr.ExpressionCommons;

public class JSONSchemaGrammarGenerator extends SchemaGrammarGenerator {

	public JSONSchemaGrammarGenerator(GrammarFile gfile) {
		super(gfile);
	}

	@Override
	public void loadPredefinedRules() {
		JSONPredefinedGrammar pre = new JSONPredefinedGrammar(gfile);
		pre.define();
	}

	@Override
	public void newRoot(String structName) {
		Expression root = _NonTerminal(structName);
		Expression[] seq = { _NonTerminal("VALUESEP"), root };
		Expression[] array = { _OpenSquare(), _S(), root, _S(), gfile.newRepetition1(seq), _S(), _CloseSquare() };
		gfile.addProduction(null, "Root", gfile.newChoice(gfile.newSequence(root), gfile.newSequence(array)));
	}

	@Override
	public void newElement(String elementName, Type t) {
		Expression[] l = { _DQuat(), t.getTypeExpression(), _DQuat(), _NonTerminal("NAMESEP"), t.next().getTypeExpression(), gfile.newOption(_NonTerminal("VALUESEP")) };
		gfile.addProduction(null, elementName, gfile.newSequence(l));
	}

	@Override
	public void newStruct(String structName, Type t) {
		Expression[] l = { _OpenWave(), _S(), t.getTypeExpression(), _S(), _CloseWave() };
		gfile.addProduction(null, structName, gfile.newSequence(l));
	}

	@Override
	public void newMembers(String structName, Type... types) {
		Expression[] l = new Expression[types.length];
		int index = 0;
		for (Type type : types) {
			l[index++] = type.getTypeExpression();
		}
		gfile.addProduction(null, structName + "_SMembers", gfile.newChoice(l));
	}

	@Override
	public void newUniqNames() {
		Expression[] l = new Expression[getMembers().size()];
		int index = 0;
		for (String name : getMembers()) {
			l[index++] = _String(name);
		}
		gfile.addProduction(null, getTableName(), gfile.newChoice(l));
	}

	@Override
	public Type newRequired(String name, Type t) {
		Expression expr = _String(name);
		return new Type(name, expr, t);
	}

	@Override
	public Type newOption(String name, Type t) {
		Expression expr = _String(name);
		return new Type(name, expr, t);
	}

	@Override
	public Type newTObject() {
		return new Type(_NonTerminal("JSONObject"));
	}

	@Override
	public Type newTStruct(String structName) {
		return new Type(_NonTerminal(structName));
	}

	@Override
	public Type newTArray(Type t) {
		Expression tExpr = t.getTypeExpression();
		Expression[] array = { _OpenSquare(), _S(), tExpr, gfile.newRepetition(_NonTerminal("VALUESEP"), tExpr), _CloseSquare() };
		return new Type(gfile.newSequence(array));
	}

	@Override
	public Type newTEnum(String[] candidates) {
		Expression[] choice = new Expression[candidates.length];
		int index = 0;
		for (String cand : candidates) {
			choice[index++] = _String(cand);
		}
		return new Type(gfile.newChoice(choice));
	}

	@Override
	public Type newTInteger() {
		return new Type(_NonTerminal("INT"));
	}

	@Override
	public Type newTFloat() {
		return new Type(_NonTerminal("Number"));
	}

	@Override
	public Type newTString() {
		return new Type(_NonTerminal("String"));
	}

	@Override
	public Type newTAny() {
		return new Type(_NonTerminal("Any"));
	}

	// FIXME
	@Override
	public Type newSet(String structName) {
		Expression[] l = new Expression[getRequiredList().size() + 1];
		int index = 1;
		l[0] = gfile.newRepetition(_NonTerminal(structName + "_SMembers"));
		for (String required : getRequiredList()) {
			l[index++] = _Exists(getTableName(), required);
		}
		return new Type(gfile.newLocal(getTableName(), l));
	}

	@Override
	public Type newUniq(String elementName, Type t) {
		Expression expr = gfile.newSequence(gfile.newAnd(_String(elementName)), gfile.newNot(_Exists(getTableName(), elementName)), _Def(getTableName()));
		return new Type(elementName, expr, t);
	}

	@Override
	public Type newAlt(String name) {
		return new Type(_NonTerminal(name));
	}

	@Override
	public Type newOthers() {
		return new Type(_NonTerminal("Member"));
	}

	@Override
	public Type newPermutation() {
		genImpliedChoice();
		if (getRequiredList().isEmpty()) {
			return newCompletePerm();
		} else {
			return newAproximatePerm();
		}
	}

	// FIXME
	private Type newCompletePerm() {
		int listLength = getRequiredList().size();

		if (listLength == 1) {
			return new Type(_NonTerminal(getRequiredList().get(0)));
		} else {
			PermutationGenerator permGen = new PermutationGenerator(listLength);
			int[][] permedList = permGen.getPermList();
			Expression[] choiceList = new Expression[permedList.length];
			int choiceCount = 0;
			for (int[] targetLine : permedList) {
				Expression[] seqList = new Expression[listLength];
				for (int index = 0; index < targetLine.length; index++) {
					seqList[index] = ExpressionCommons.newNonTerminal(null, gfile, getRequiredList().get(targetLine[index]));
				}
				choiceList[choiceCount++] = gfile.newSequence(seqList);
			}
			return new Type(gfile.newChoice(choiceList));
		}
	}

	// FIXME
	private Type newAproximatePerm() {
		int listLength = getRequiredList().size();
		if (listLength == 0) {
			return new Type(_NonTerminal(getTableName() + "_implied"));
		} else {
			PermutationGenerator permGen = new PermutationGenerator(listLength);
			int[][] permutedList = permGen.getPermList();
			Expression[] choiceList = new Expression[permutedList.length];
			Expression impliedChoiceRule = _NonTerminal(getTableName() + "_implied");
			int choiceCount = 0;
			for (int[] targetLine : permutedList) {
				int seqCount = 0;
				Expression[] seqList = new Expression[listLength * 2 + 1];
				seqList[seqCount++] = gfile.newRepetition(impliedChoiceRule);
				for (int index = 0; index < targetLine.length; index++) {
					seqList[seqCount++] = ExpressionCommons.newNonTerminal(null, gfile, getRequiredList().get(targetLine[index]));
					seqList[seqCount++] = gfile.newRepetition(impliedChoiceRule);
				}
				choiceList[choiceCount++] = gfile.newSequence(seqList);
			}
			return new Type(gfile.newChoice(choiceList));
		}

	}

	private final void genImpliedChoice() {
		List<String> impliedList = extractImpliedMembers();
		Expression[] l = new Expression[impliedList.size()];
		int choiceCount = 0;
		for (String nonTerminalSymbol : impliedList) {
			l[choiceCount++] = _NonTerminal(nonTerminalSymbol);
		}
		gfile.addProduction(null, getTableName() + "_implied", gfile.newChoice(l));
	}

	private final Expression _OpenSquare() {
		return gfile.newByteChar('[');
	}

	private final Expression _CloseSquare() {
		return gfile.newByteChar(']');
	}

	private final Expression _OpenWave() {
		return gfile.newByteChar('{');
	}

	private final Expression _CloseWave() {
		return gfile.newByteChar('}');
	}

}
