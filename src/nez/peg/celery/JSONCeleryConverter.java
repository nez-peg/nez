package nez.peg.celery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import nez.ast.AbstractTree;
import nez.ast.Tag;
import nez.lang.Expression;
import nez.lang.GrammarFactory;
import nez.util.UList;

public class JSONCeleryConverter extends AbstractCeleryConverter {

	private HashMap<String, List<String>> classMap;
	private List<String> requiredPropertiesList;
	private List<String> impliedPropertiesList;
	private String currentClassName;
	private boolean useExtension = true;

	public JSONCeleryConverter() {
		this.classMap = new HashMap<>();
	}

	public JSONCeleryConverter(boolean useExtension) {
		this.classMap = new HashMap<>();
		this.useExtension = useExtension;
	}

	@Override
	protected final void loadPredefinedRules(AbstractTree<?> node) {
		JSONPredefinedRules preRules = new JSONPredefinedRules(grammar);
		preRules.defineRule();
	}

	// visitor methods

	@Override
	public final void visitRoot(AbstractTree<?> node) {
		for (AbstractTree<?> classNode : node) {
			initPropertiesList();
			currentClassName = classNode.getText(0, null);
			this.visit("visit", classNode);
			if (useExtension) {
				grammar.defineProduction(classNode, currentClassName, genExClassRule(currentClassName));
			} else {
				grammar.defineProduction(classNode, currentClassName, genClassRule(currentClassName));
			}
			savePropertiesList(currentClassName);
		}
		grammar.defineProduction(node, "Root", genRootClass());
	}

	@Override
	public final void visitStruct(AbstractTree<?> node) {
		for (AbstractTree<?> memberNode : node) {
			this.visit("visit", memberNode);
		}
	}

	@Override
	public final void visitRequired(AbstractTree<?> node) {
		String propertyName = node.getText(0, null);
		requiredPropertiesList.add(propertyName);
		Expression[] seq = { _DQuoat(), GrammarFactory.newDefSymbol(node, grammar, Tag.tag(currentClassName), GrammarFactory.newString(node, propertyName)), _DQuoat(), GrammarFactory.newNonTerminal(null, grammar, "NAMESEP"), toExpression(node.get(1)),
				grammar.newOption(GrammarFactory.newNonTerminal(null, grammar, "VALUESEP")) };
		grammar.defineProduction(node, node.getText(0, null), grammar.newSequence(seq));
	}

	@Override
	public final void visitOption(AbstractTree<?> node) {
		String propertyName = node.getText(0, null);
		impliedPropertiesList.add(propertyName);
		Expression[] seq = { _DQuoat(), grammar.newString(propertyName), _DQuoat(), GrammarFactory.newNonTerminal(null, grammar, "NAMESEP"), toExpression(node.get(1)), grammar.newOption(GrammarFactory.newNonTerminal(null, grammar, "VALUESEP")) };
		grammar.defineProduction(node, node.getText(0, null), grammar.newSequence(seq));
	}

	@Override
	public final void visitUntypedRequired(AbstractTree<?> node) {
		String propertyName = node.getText(0, null);
		requiredPropertiesList.add(propertyName);
		// inferType(node.get(2));
		Expression[] seq = { _DQuoat(), GrammarFactory.newDefSymbol(node, grammar, Tag.tag(currentClassName), GrammarFactory.newString(node, propertyName)), _DQuoat(), GrammarFactory.newNonTerminal(null, grammar, "NAMESEP"),
				GrammarFactory.newNonTerminal(null, grammar, "Any"), grammar.newOption(GrammarFactory.newNonTerminal(null, grammar, "VALUESEP")) };
		grammar.defineProduction(node, node.getText(0, null), grammar.newSequence(seq));
	}

	@Override
	public final void visitUntypedOption(AbstractTree<?> node) {
		String propertyName = node.getText(0, null);
		impliedPropertiesList.add(propertyName);
		// inferType(node.get(2));
		Expression[] seq = { _DQuoat(), grammar.newString(propertyName), _DQuoat(), GrammarFactory.newNonTerminal(null, grammar, "NAMESEP"), GrammarFactory.newNonTerminal(null, grammar, "Any"),
				grammar.newOption(GrammarFactory.newNonTerminal(null, grammar, "VALUESEP")) };
		grammar.defineProduction(node, node.getText(0, null), grammar.newSequence(seq));
	}

	public final void visitName(AbstractTree<?> node) {
	}

	// to Expression Methods

	@Override
	public final Expression toTObject(AbstractTree<?> node) {
		return GrammarFactory.newNonTerminal(null, grammar, "JSONObject");
	}

	@Override
	public final Expression toTClass(AbstractTree<?> node) {
		return GrammarFactory.newNonTerminal(null, grammar, node.toText());
	}

	@Override
	public final Expression toTArray(AbstractTree<?> node) {
		Expression type = toExpression(node.get(0));
		Expression[] seq = { grammar.newByteChar('['), _SPACING(), type, grammar.newRepetition(GrammarFactory.newNonTerminal(null, grammar, "VALUESEP"), type), grammar.newByteChar(']') };
		return grammar.newSequence(seq);
	}

	@Override
	public final Expression toTEnum(AbstractTree<?> node) {
		Expression[] choice = new Expression[node.size()];
		for (int index = 0; index < choice.length; index++) {
			choice[index] = grammar.newString(node.getText(index, null));
		}
		return grammar.newChoice(choice);
	}

	// generator methods
	@Deprecated
	private final Expression genClassRule(String className) {
		String memberList = className + "_Members";
		Expression[] seq = { _DQuoat(), grammar.newString(className), _DQuoat(), GrammarFactory.newNonTerminal(null, grammar, "NAMESEP"), grammar.newByteChar('{'), _SPACING(), GrammarFactory.newNonTerminal(null, grammar, memberList), _SPACING(),
				grammar.newByteChar('}') };
		grammar.defineProduction(null, memberList, genMemberRule(className));
		return grammar.newSequence(seq);
	}

	@Deprecated
	private final Expression genMemberRule(String className) {
		if (impliedPropertiesList.isEmpty()) {
			return genCompMember();
		} else {
			String impliedChoiceRuleName = className + "_imp";
			genImpliedChoice(impliedChoiceRuleName);
			return genProxMember(impliedChoiceRuleName);
		}
	}

	@Deprecated
	private final Expression genCompMember() {
		int listLength = requiredPropertiesList.size();

		// return the rule that include only one member
		if (listLength == 1) {
			return GrammarFactory.newNonTerminal(null, grammar, requiredPropertiesList.get(0));
		}

		// return the rule that include permuted members
		else {
			int[][] permutedList = permute(listLength);
			Expression[] choiceList = new Expression[permutedList.length];
			int choiceCount = 0;
			for (int[] targetLine : permutedList) {
				Expression[] seqList = new Expression[listLength];
				for (int index = 0; index < targetLine.length; index++) {
					seqList[index] = GrammarFactory.newNonTerminal(null, grammar, requiredPropertiesList.get(targetLine[index]));
				}
				choiceList[choiceCount++] = grammar.newSequence(seqList);
			}
			return grammar.newChoice(choiceList);
		}
	}

	@Deprecated
	private final Expression genProxMember(String impliedChoiceRuleName) {
		int listLength = requiredPropertiesList.size();

		// return the rule that include only implied member list
		if (listLength == 0) {
			return GrammarFactory.newNonTerminal(null, grammar, impliedChoiceRuleName);
		}

		// return the rule that include mixed member list
		else {
			int[][] permutedList = permute(listLength);
			Expression[] choiceList = new Expression[permutedList.length];
			Expression impliedChoiceRule = GrammarFactory.newNonTerminal(null, grammar, impliedChoiceRuleName);
			int choiceCount = 0;
			for (int[] targetLine : permutedList) {
				int seqCount = 0;
				Expression[] seqList = new Expression[listLength * 2 + 1];
				seqList[seqCount++] = grammar.newRepetition(impliedChoiceRule);
				for (int index = 0; index < targetLine.length; index++) {
					seqList[seqCount++] = GrammarFactory.newNonTerminal(null, grammar, requiredPropertiesList.get(targetLine[index]));
					seqList[seqCount++] = grammar.newRepetition(impliedChoiceRule);
				}
				choiceList[choiceCount++] = grammar.newSequence(seqList);
			}
			return grammar.newChoice(choiceList);
		}
	}

	private final Expression genExClassRule(String className) {
		int requiredListSize = requiredPropertiesList.size();
		String memberList = className + "_Members";

		Expression[] tables = new Expression[requiredListSize];
		for (int i = 0; i < requiredListSize; i++) {
			tables[i] = grammar.newExists(className, requiredPropertiesList.get(i));
		}

		Expression[] seq = { _DQuoat(), grammar.newString(className), _DQuoat(), _SPACING(), GrammarFactory.newNonTerminal(null, grammar, "NAMESEP"), grammar.newByteChar('{'), _SPACING(),
				grammar.newBlock(grammar.newRepetition1(GrammarFactory.newNonTerminal(null, grammar, memberList)), grammar.newSequence(tables)), _SPACING(), grammar.newByteChar('}') };
		grammar.defineProduction(null, memberList, genExMemberRule(className, requiredListSize));

		// return grammar.newLocal(className, seq);
		return GrammarFactory.newLocal(null, grammar, Tag.tag(className), grammar.newSequence(seq));
	}

	private final Expression genExMemberRule(String className, int requiredListSize) {
		UList<Expression> choice = new UList<>(new Expression[10]);
		String impliedChoiceRuleName = className + "_imp";

		for (int i = 0; i < requiredListSize; i++) {
			String memberName = requiredPropertiesList.get(i);
			Expression required = grammar.newSequence(grammar.newNot(grammar.newExists(className, memberName), GrammarFactory.newNonTerminal(null, grammar, memberName)));
			GrammarFactory.addChoice(choice, required);
		}

		if (!impliedPropertiesList.isEmpty()) {
			genImpliedChoice(impliedChoiceRuleName);
			GrammarFactory.addChoice(choice, GrammarFactory.newNonTerminal(null, grammar, impliedChoiceRuleName));
		}

		GrammarFactory.addChoice(choice, GrammarFactory.newNonTerminal(null, grammar, "Any"));
		return GrammarFactory.newChoice(null, choice);
	}

	private final void genImpliedChoice(String ruleName) {
		Expression[] l = new Expression[impliedPropertiesList.size()];
		int choiceCount = 0;
		for (String nonTerminalSymbol : impliedPropertiesList) {
			l[choiceCount++] = GrammarFactory.newNonTerminal(null, grammar, nonTerminalSymbol);
		}
		grammar.defineProduction(null, ruleName, grammar.newChoice(l));
	}

	private final Expression genRootClass() {
		Expression root = genRootSeq();
		Expression[] seq = { GrammarFactory.newNonTerminal(null, grammar, "VALUESEP"), root };
		Expression[] array = { grammar.newByteChar('['), _SPACING(), root, _SPACING(), grammar.newRepetition1(seq), _SPACING(), grammar.newByteChar(']') };
		return grammar.newChoice(grammar.newSequence(root), grammar.newSequence(array));
	}

	private final Expression genRootSeq() {
		List<String> rootMemberList = classMap.get(rootClassName);
		int requiredMembersListSize = rootMemberList.size();
		String membersNonterminal = rootClassName + "_Members";

		Expression[] tables = new Expression[requiredMembersListSize];
		for (int i = 0; i < requiredMembersListSize; i++) {
			tables[i] = grammar.newExists(rootMemberList.get(i), null);
		}

		Expression[] seq = { grammar.newByteChar('{'), _SPACING(), grammar.newBlock(grammar.newRepetition1(GrammarFactory.newNonTerminal(null, grammar, membersNonterminal)), grammar.newSequence(tables)), _SPACING(), grammar.newByteChar('}') };
		return grammar.newSequence(seq);
	}

	// Utilities

	private final void initPropertiesList() {
		requiredPropertiesList = new ArrayList<String>();
		impliedPropertiesList = new ArrayList<String>();
	}

	private final void savePropertiesList(String className) {
		this.classMap.put(className, requiredPropertiesList);
	}

	private final int[][] permute(int listLength) {
		int[] target = new int[listLength];
		for (int i = 0; i < target.length; i++) {
			target[i] = i;
		}
		PermutationGen permGen = new PermutationGen(target);
		return permGen.getPermList();
	}

	private final Expression _SPACING() {
		return GrammarFactory.newNonTerminal(null, grammar, "SPACING");
	}

	private final Expression _DQuoat() {
		return grammar.newByteChar('"');
	}

}
