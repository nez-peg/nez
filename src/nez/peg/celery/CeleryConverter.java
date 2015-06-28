package nez.peg.celery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import nez.NezOption;
import nez.NezException;
import nez.SourceContext;
import nez.ast.CommonTree;
import nez.ast.CommonTreeVisitor;
import nez.lang.Expression;
import nez.lang.Grammar;
import nez.lang.GrammarChecker;
import nez.lang.GrammarFile;
import nez.util.ConsoleUtils;

public class CeleryConverter extends CommonTreeVisitor {

	static GrammarFile celeryGrammar = null;
	private GrammarFile grammar;
	private HashMap<String, Integer> classNameMap;
	private List<String> requiredMembersList;
	private List<String> impliedMemebersList;
	private String rootClassName;
	private final boolean UseExtendedSyntax = true;

	public CeleryConverter() {
		this.classNameMap = new HashMap<>();
	}

	public final static GrammarFile loadGrammar(String filePath, NezOption option) throws IOException {
		if (celeryGrammar == null) {
			try {
				celeryGrammar = GrammarFile.loadGrammarFile("celery.nez", NezOption.newDefaultOption());
			} 
			catch (IOException e) {
				ConsoleUtils.exit(1, "can't load celery.nez");
			}
		}
		Grammar p = celeryGrammar.newGrammar("File");
		SourceContext celeryFile = SourceContext.newFileContext(filePath);
		CommonTree node = p.parse(celeryFile);
		if (node == null) {
			throw new NezException(celeryFile.getSyntaxErrorMessage());
		}
		if (celeryFile.hasUnconsumed()) {
			throw new NezException(celeryFile.getUnconsumedMessage());
		}
		CeleryConverter converter = new CeleryConverter();
		converter.setRootClassName(filePath);
		GrammarFile gfile = GrammarFile.newGrammarFile(filePath, option);
		converter.convert(node, gfile);
		gfile.verify();
		return gfile;
	}

	private final void loadPredefinedRules(CommonTree node) {
		JSONPredefinedRules preRules = new JSONPredefinedRules(grammar, rootClassName);
		preRules.defineRule();
	}

	private final void convert(CommonTree node, GrammarFile grammar) {
		this.grammar = grammar;
		loadPredefinedRules(node);
		this.visit("visit", node);
	}

	// visitor methods

	public final void visitRoot(CommonTree node) {
		correctClassNames(node);
		for (CommonTree classNode : node) {
			initMemberList();
			this.visit("visit", classNode);
			String className = classNode.textAt(0, null);
			if (UseExtendedSyntax) {
				grammar.defineProduction(classNode, className, genExClassRule(className));
			} else {
				grammar.defineProduction(classNode, className, genClassRule(className));
			}
		}
		grammar.defineProduction(node, "Root", genRootClass());
	}

	public final void visitClass(CommonTree node) {
		for (CommonTree memberNode : node) {
			this.visit("visit", memberNode);
		}
	}

	public final void visitRequired(CommonTree node) {
		String name = node.textAt(0, null);
		requiredMembersList.add(name);
		Expression[] seq = {
				_DQuoat(),
				grammar.newString(name),
				_DQuoat(),
				grammar.newNonTerminal("NAMESEP"),
				toExpression(node.get(1)),
				grammar.newOption(grammar.newNonTerminal("VALUESEP"))
		};
		grammar.defineProduction(node, node.textAt(0, null), grammar.newSequence(seq));
	}

	public final void visitOption(CommonTree node) {
		String name = node.textAt(0, null);
		impliedMemebersList.add(name);
		Expression[] seq = {
				_DQuoat(),
				grammar.newString(name),
				_DQuoat(),
				grammar.newNonTerminal("NAMESEP"),
				toExpression(node.get(1)),
				grammar.newOption(grammar.newNonTerminal("VALUESEP"))
		};
		grammar.defineProduction(node, node.textAt(0, null), grammar.newSequence(seq));
	}

	public final void visitUntypedRequired(CommonTree node) {
		String name = node.textAt(0, null);
		requiredMembersList.add(name);
		// inferType(node.get(2));
		Expression[] seq = {
				_DQuoat(),
				grammar.newString(name),
				_DQuoat(),
				grammar.newNonTerminal("NAMESEP"),
				grammar.newNonTerminal("Any"),
				grammar.newOption(grammar.newNonTerminal("VALUESEP"))
		};
		grammar.defineProduction(node, node.textAt(0, null), grammar.newSequence(seq));
	}

	public final void visitUntypedOption(CommonTree node) {
		String name = node.textAt(0, null);
		impliedMemebersList.add(name);
		// inferType(node.get(2));
		Expression[] seq = {
				_DQuoat(),
				grammar.newString(name),
				_DQuoat(),
				grammar.newNonTerminal("NAMESEP"),
				grammar.newNonTerminal("Any"),
				grammar.newOption(grammar.newNonTerminal("VALUESEP"))
		};
		grammar.defineProduction(node, node.textAt(0, null), grammar.newSequence(seq));
	}

	public final void visitName(CommonTree node) {
	}

	public final Expression visitTEnum(CommonTree node) {
		Expression[] choice = new Expression[node.size()];
		for (int index = 0; index < choice.length; index++) {
			choice[index] = grammar.newString(node.textAt(index, null));
		}
		return grammar.newChoice(choice);
	}

	// to Expression Methods

	private final Expression toExpression(CommonTree node) {
		return (Expression) this.visit("to", node);
	}

	public final Expression toTBoolean(CommonTree node) {
		return grammar.newNonTerminal("BOOLEAN");
	}

	public final Expression toTInteger(CommonTree node) {
		return grammar.newNonTerminal("INT");
	}

	public final Expression toTFloat(CommonTree node) {
		return grammar.newNonTerminal("FLOAT");
	}

	public final Expression toTString(CommonTree node) {
		return grammar.newNonTerminal("STRING");
	}

	public final Expression toTAny(CommonTree node) {
		return grammar.newNonTerminal("Any");
	}

	public final Expression toTObject(CommonTree node) {
		return grammar.newNonTerminal("JSONObject");
	}

	public final Expression toTClass(CommonTree node) {
		return grammar.newNonTerminal(node.getText());
	}

	public final Expression toTArray(CommonTree node) {
		Expression type = toExpression(node.get(0));
		Expression[] seq = {
				grammar.newByteChar('['),
				_SPACING(),
				type,
				grammar.newRepetition(grammar.newNonTerminal("VALUESEP"), type),
				grammar.newByteChar(']')
		};
		return grammar.newSequence(seq);
	}

	// generator methods

	private final Expression genClassRule(String className) {
		String memberList = className + "_Members";
		Expression[] seq = {
				_DQuoat(),
				grammar.newString(className),
				_DQuoat(),
				grammar.newNonTerminal("NAMESEP"),
				grammar.newByteChar('{'),
				_SPACING(),
				grammar.newNonTerminal(memberList),
				_SPACING(),
				grammar.newByteChar('}')
		};
		grammar.defineProduction(null, memberList, genMemberRule(className));
		return grammar.newSequence(seq);
	}

	private final Expression genMemberRule(String className) {
		if (impliedMemebersList.isEmpty()) {
			return genCompMember();
		} else {
			String impliedChoiceRuleName = className + "_imp";
			genImpliedChoice(impliedChoiceRuleName);
			return genProxMember(impliedChoiceRuleName);
		}
	}

	private final Expression genCompMember() {
		int listLength = requiredMembersList.size();

		// return the rule that include only one member
		if (listLength == 1) {
			return grammar.newNonTerminal(requiredMembersList.get(0));
		}

		// return the rule that include permuted members
		else {
			int[][] permutedList = permute(listLength);
			Expression[] choiceList = new Expression[permutedList.length];
			int choiceCount = 0;
			for (int[] targetLine : permutedList) {
				Expression[] seqList = new Expression[listLength];
				for (int index = 0; index < targetLine.length; index++) {
					seqList[index] = grammar.newNonTerminal(requiredMembersList
							.get(targetLine[index]));
				}
				choiceList[choiceCount++] = grammar.newSequence(seqList);
			}
			return grammar.newChoice(choiceList);
		}
	}

	private final Expression genProxMember(String impliedChoiceRuleName) {
		int listLength = requiredMembersList.size();

		// return the rule that include only implied member list
		if (listLength == 0) {
			return grammar.newNonTerminal(impliedChoiceRuleName);
		}

		// return the rule that include mixed member list
		else {
			int[][] permutedList = permute(listLength);
			Expression[] choiceList = new Expression[permutedList.length];
			Expression impliedChoiceRule = grammar.newNonTerminal(impliedChoiceRuleName);
			int choiceCount = 0;
			for (int[] targetLine : permutedList) {
				int seqCount = 0;
				Expression[] seqList = new Expression[listLength * 2 + 1];
				seqList[seqCount++] = grammar.newRepetition(impliedChoiceRule);
				for (int index = 0; index < targetLine.length; index++) {
					seqList[seqCount++] = grammar.newNonTerminal(requiredMembersList
							.get(targetLine[index]));
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
			tables[i] = grammar.newExists(requiredMembersList.get(i));
		}

		Expression[] seq = {
				_DQuoat(),
				grammar.newString(className),
				_DQuoat(),
				_SPACING(),
				grammar.newNonTerminal("NAMESEP"),
				grammar.newByteChar('{'),
				_SPACING(),
				grammar.newRepetition1(grammar.newNonTerminal(memberList)),
				grammar.newSequence(tables),
				_SPACING(),
				grammar.newByteChar('}')
		};
		grammar.defineProduction(null, memberList,
				genExMemberRule(className, requiredMembersListSize));
		return grammar.newSequence(seq);
	}

	private final Expression genExMemberRule(String className, int requiredListSize) {
		Expression[] choice = new Expression[requiredListSize + 1];
		String impliedChoiceRuleName = className + "_imp";
		genImpliedChoice(impliedChoiceRuleName);
		for (int i = 0; i < requiredListSize; i++) {
			String memberName = requiredMembersList.get(i);
			choice[i] = grammar.newSequence(grammar.newNot(grammar.newIsSymbol(memberName)),
					grammar.newDefSymbol(memberName, grammar.newNonTerminal(memberName)));
		}
		choice[requiredListSize] = grammar.newNonTerminal(impliedChoiceRuleName);

		return grammar.newChoice(choice);
	}

	private final void genImpliedChoice(String ruleName) {
		Expression[] l = new Expression[impliedMemebersList.size()];
		int choiceCount = 0;
		for (String nonTerminalSymbol : impliedMemebersList) {
			l[choiceCount++] = grammar.newNonTerminal(nonTerminalSymbol);
		}
		grammar.defineProduction(null, ruleName, grammar.newChoice(l));
	}

	private final Expression genRootClass() {
		Expression root = genRootSeq();
		Expression[] seq = {
				grammar.newNonTerminal("VALUESEP"), root
		};
		Expression[] array = {
				grammar.newByteChar('['),
				_SPACING(),
				root,
				grammar.newRepetition1(seq),
				_SPACING(),
				grammar.newByteChar(']')
		};
		return grammar.newChoice(grammar.newSequence(root), grammar.newSequence(array));
	}

	private final Expression genRootSeq() {
		int requiredMembersListSize = requiredMembersList.size();
		String memberList = rootClassName + "_Members";

		Expression[] tables = new Expression[requiredMembersListSize];
		for (int i = 0; i < requiredMembersListSize; i++) {
			tables[i] = grammar.newExists(requiredMembersList.get(i));
		}

		Expression[] seq = {
				grammar.newByteChar('{'),
				_SPACING(),
				grammar.newRepetition1(grammar.newNonTerminal(memberList)),
				grammar.newSequence(tables),
				_SPACING(),
				grammar.newByteChar('}')
		};
		return grammar.newSequence(seq);
	}

	// Utilities

	private final void correctClassNames(CommonTree node) {
		int index = 0;
		for (CommonTree classNode : node) {
			String className = classNode.textAt(0, null);
			classNameMap.put(className, index++);
		}
	}

	private final void setRootClassName(String filePath) {
		int offset = filePath.lastIndexOf('/');
		int end = filePath.indexOf('.');
		this.rootClassName = filePath.substring(offset + 1, end);
	}

	private final void initMemberList() {
		requiredMembersList = new ArrayList<String>();
		impliedMemebersList = new ArrayList<String>();
	}

	private final int[][] permute(int listLength) {
		int[] target = new int[listLength];
		for (int i = 0; i < target.length; i++) {
			target[i] = i;
		}
		PermutationGen permGen = new PermutationGen(target);
		return permGen.getPermList();
	}

	private final Expression _SPACING(){
		return grammar.newNonTerminal("SPACING");
	}

	private final Expression _DQuoat() {
		return grammar.newByteChar('"');
	}


}
