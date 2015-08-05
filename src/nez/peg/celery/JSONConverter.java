package nez.peg.celery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import nez.NezException;
import nez.NezOption;
import nez.SourceContext;
import nez.ast.AbstractTreeVisitor;
import nez.ast.CommonTree;
import nez.ast.Tag;
import nez.lang.Expression;
import nez.lang.Grammar;
import nez.lang.GrammarFactory;
import nez.lang.GrammarFile;
import nez.util.ConsoleUtils;
import nez.util.UList;

public class JSONConverter extends AbstractTreeVisitor {

	static GrammarFile celeryGrammar = null;
	private HashMap<String, List<String>> classMap;
	private List<String> requiredMembersList;
	private List<String> impliedMemebersList;
	private final boolean UseExtendedGrammar = true;

	public JSONConverter() {
		this.classMap = new HashMap<>();
	}

	public final static GrammarFile loadGrammar(String filePath, NezOption option) throws IOException {
		option.setOption("notice", false);
		if (celeryGrammar == null) {
			try {
				celeryGrammar = GrammarFile.loadGrammarFile("celery.nez", NezOption.newDefaultOption());
			} catch (IOException e) {
				ConsoleUtils.exit(1, "can't load celery.nez");
			}
		}
		Grammar p = celeryGrammar.newGrammar("File");
		SourceContext celeryFile = SourceContext.newFileContext(filePath);
		CommonTree node = p.parseCommonTree(celeryFile);
		if (node == null) {
			throw new NezException(celeryFile.getSyntaxErrorMessage());
		}
		if (celeryFile.hasUnconsumed()) {
			throw new NezException(celeryFile.getUnconsumedMessage());
		}
		JSONConverter converter = new JSONConverter();
		converter.setRootClassName(filePath);
		GrammarFile gfile = GrammarFile.newGrammarFile(filePath, option);
		converter.convert(node, gfile);
		gfile.verify();
		return gfile;
	}

	protected final void loadPredefinedRules(CommonTree node) {
		JSONPredefinedRules preRules = new JSONPredefinedRules(grammar);
		preRules.defineRule();
	}

	// public final void convert(CommonTree node, GrammarFile grammar) {
	// this.grammar = grammar;
	// loadPredefinedRules(node);
	// this.visit("visit", node);
	// }

	// visitor methods

	public final void visitRoot(CommonTree node) {
		for (CommonTree classNode : node) {
			initMemberList();
			this.visit("visit", classNode);
			String className = classNode.getText(0, null);
			if (UseExtendedGrammar) {
				grammar.defineProduction(classNode, className, genExClassRule(className));
			} else {
				grammar.defineProduction(classNode, className, genClassRule(className));
			}
			saveMemberList(className);
		}
		grammar.defineProduction(node, "Root", genRootClass());
	}

	public final void visitStruct(CommonTree node) {
		for (CommonTree memberNode : node) {
			this.visit("visit", memberNode);
		}
	}

	public final void visitRequired(CommonTree node) {
		String name = node.getText(0, null);
		requiredMembersList.add(name);
		Expression[] seq = { _DQuoat(), grammar.newString(name), _DQuoat(), GrammarFactory.newNonTerminal(null, grammar, "NAMESEP"), toExpression(node.get(1)), grammar.newOption(GrammarFactory.newNonTerminal(null, grammar, "VALUESEP")) };
		grammar.defineProduction(node, node.getText(0, null), grammar.newSequence(seq));
	}

	public final void visitOption(CommonTree node) {
		String name = node.getText(0, null);
		impliedMemebersList.add(name);
		Expression[] seq = { _DQuoat(), grammar.newString(name), _DQuoat(), GrammarFactory.newNonTerminal(null, grammar, "NAMESEP"), toExpression(node.get(1)), grammar.newOption(GrammarFactory.newNonTerminal(null, grammar, "VALUESEP")) };
		grammar.defineProduction(node, node.getText(0, null), grammar.newSequence(seq));
	}

	public final void visitUntypedRequired(CommonTree node) {
		String name = node.getText(0, null);
		requiredMembersList.add(name);
		// inferType(node.get(2));
		Expression[] seq = { _DQuoat(), grammar.newString(name), _DQuoat(), GrammarFactory.newNonTerminal(null, grammar, "NAMESEP"), GrammarFactory.newNonTerminal(null, grammar, "Any"),
				grammar.newOption(GrammarFactory.newNonTerminal(null, grammar, "VALUESEP")) };
		grammar.defineProduction(node, node.getText(0, null), grammar.newSequence(seq));
	}

	public final void visitUntypedOption(CommonTree node) {
		String name = node.getText(0, null);
		impliedMemebersList.add(name);
		// inferType(node.get(2));
		Expression[] seq = { _DQuoat(), grammar.newString(name), _DQuoat(), GrammarFactory.newNonTerminal(null, grammar, "NAMESEP"), GrammarFactory.newNonTerminal(null, grammar, "Any"),
				grammar.newOption(GrammarFactory.newNonTerminal(null, grammar, "VALUESEP")) };
		grammar.defineProduction(node, node.getText(0, null), grammar.newSequence(seq));
	}

	public final void visitName(CommonTree node) {
	}

	public final Expression toTEnum(CommonTree node) {
		Expression[] choice = new Expression[node.size()];
		for (int index = 0; index < choice.length; index++) {
			choice[index] = grammar.newString(node.getText(index, null));
		}
		return grammar.newChoice(choice);
	}

	// to Expression Methods

	private final Expression toExpression(CommonTree node) {
		return (Expression) this.visit("to", node);
	}

	public final Expression toTBoolean(CommonTree node) {
		return GrammarFactory.newNonTerminal(null, grammar, "BOOLEAN");
	}

	public final Expression toTInteger(CommonTree node) {
		return GrammarFactory.newNonTerminal(null, grammar, "INT");
	}

	public final Expression toTFloat(CommonTree node) {
		return GrammarFactory.newNonTerminal(null, grammar, "Number");
	}

	public final Expression toTString(CommonTree node) {
		return GrammarFactory.newNonTerminal(null, grammar, "String");
	}

	public final Expression toTAny(CommonTree node) {
		return GrammarFactory.newNonTerminal(null, grammar, "Any");
	}

	public final Expression toTObject(CommonTree node) {
		return GrammarFactory.newNonTerminal(null, grammar, "JSONObject");
	}

	public final Expression toTClass(CommonTree node) {
		return GrammarFactory.newNonTerminal(null, grammar, node.toText());
	}

	public final Expression toTArray(CommonTree node) {
		Expression type = toExpression(node.get(0));
		Expression[] seq = { grammar.newByteChar('['), _SPACING(), type, grammar.newRepetition(GrammarFactory.newNonTerminal(null, grammar, "VALUESEP"), type), grammar.newByteChar(']') };
		return grammar.newSequence(seq);
	}

	public final Expression toTEnum(CommonTree node) {
		Expression[] choice = new Expression[node.size()];
		for (int index = 0; index < choice.length; index++) {
			choice[index] = grammar.newString(node.textAt(index, null));
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
		if (impliedMemebersList.isEmpty()) {
			return genCompMember();
		} else {
			String impliedChoiceRuleName = className + "_imp";
			genImpliedChoice(impliedChoiceRuleName);
			return genProxMember(impliedChoiceRuleName);
		}
	}

	@Deprecated
	private final Expression genCompMember() {
		int listLength = requiredMembersList.size();

		// return the rule that include only one member
		if (listLength == 1) {
			return GrammarFactory.newNonTerminal(null, grammar, requiredMembersList.get(0));
		}

		// return the rule that include permuted members
		else {
			int[][] permutedList = permute(listLength);
			Expression[] choiceList = new Expression[permutedList.length];
			int choiceCount = 0;
			for (int[] targetLine : permutedList) {
				Expression[] seqList = new Expression[listLength];
				for (int index = 0; index < targetLine.length; index++) {
					seqList[index] = GrammarFactory.newNonTerminal(null, grammar, requiredMembersList.get(targetLine[index]));
				}
				choiceList[choiceCount++] = grammar.newSequence(seqList);
			}
			return grammar.newChoice(choiceList);
		}
	}

	@Deprecated
	private final Expression genProxMember(String impliedChoiceRuleName) {
		int listLength = requiredMembersList.size();

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
					seqList[seqCount++] = GrammarFactory.newNonTerminal(null, grammar, requiredMembersList.get(targetLine[index]));
					seqList[seqCount++] = grammar.newRepetition(impliedChoiceRule);
				}
				choiceList[choiceCount++] = grammar.newSequence(seqList);
			}
			return grammar.newChoice(choiceList);
		}
	}

	private final Expression genExClassRule(String className) {
		int requiredMembersListSize = requiredMembersList.size();
		String memberList = className + "_Members";

		Expression[] tables = new Expression[requiredMembersListSize];
		for (int i = 0; i < requiredMembersListSize; i++) {
			tables[i] = grammar.newExists(requiredMembersList.get(i), null /* FIXME */);
		}

		Expression[] seq = { _DQuoat(), grammar.newString(className), _DQuoat(), _SPACING(), GrammarFactory.newNonTerminal(null, grammar, "NAMESEP"), grammar.newByteChar('{'), _SPACING(),
				grammar.newBlock(grammar.newRepetition1(GrammarFactory.newNonTerminal(null, grammar, memberList)), grammar.newSequence(tables)), _SPACING(), grammar.newByteChar('}') };
		grammar.defineProduction(null, memberList, genExMemberRule(className, requiredMembersListSize));
		return grammar.newSequence(seq);
	}

	private final Expression genExMemberRule(String className, int requiredListSize) {
		Expression[] values = new Expression[10];
		UList<Expression> choice = new UList<>(values);
		String impliedChoiceRuleName = className + "_imp";

		for (int i = 0; i < requiredListSize; i++) {
			String memberName = requiredMembersList.get(i);
			Expression required = grammar
					.newSequence(grammar.newNot(GrammarFactory.newIsSymbol(null, grammar, Tag.tag(memberName))), GrammarFactory.newDefSymbol(null, grammar, Tag.tag(memberName), GrammarFactory.newNonTerminal(null, grammar, memberName)));
			GrammarFactory.addChoice(choice, required);
		}

		if (!impliedMemebersList.isEmpty()) {
			genImpliedChoice(impliedChoiceRuleName);
			GrammarFactory.addChoice(choice, GrammarFactory.newNonTerminal(null, grammar, impliedChoiceRuleName));
		}

		GrammarFactory.addChoice(choice, GrammarFactory.newNonTerminal(null, grammar, "Any"));
		return GrammarFactory.newChoice(null, choice);
	}

	private final void genImpliedChoice(String ruleName) {
		Expression[] l = new Expression[impliedMemebersList.size()];
		int choiceCount = 0;
		for (String nonTerminalSymbol : impliedMemebersList) {
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
			tables[i] = grammar.newExists(requiredMembersList.get(i), null);
		}

		Expression[] seq = { grammar.newByteChar('{'), _SPACING(), grammar.newBlock(grammar.newRepetition1(GrammarFactory.newNonTerminal(null, grammar, membersNonterminal)), grammar.newSequence(tables)), _SPACING(), grammar.newByteChar('}') };
		return grammar.newSequence(seq);
	}

	// Utilities

	private final void initMemberList() {
		requiredMembersList = new ArrayList<String>();
		impliedMemebersList = new ArrayList<String>();
	}

	private final void saveMemberList(String className) {
		this.classMap.put(className, requiredMembersList);
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
