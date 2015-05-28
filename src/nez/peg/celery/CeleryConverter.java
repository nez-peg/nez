package nez.peg.celery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import nez.NezException;
import nez.SourceContext;
import nez.ast.CommonTree;
import nez.ast.CommonTreeVisitor;
import nez.lang.Expression;
import nez.lang.Grammar;
import nez.lang.GrammarChecker;
import nez.lang.NameSpace;
import nez.util.ConsoleUtils;

public class CeleryConverter extends CommonTreeVisitor {

	static NameSpace celeryGrammar = null;
	private NameSpace grammar;
	private HashMap<String, Integer> classNameMap;
	private List<String> requiredMembersList;
	private List<String> impliedMemebersList;
	private String rootClassName;

	public CeleryConverter() {
		this.classNameMap = new HashMap<>();
	}

	public final static NameSpace loadGrammar(String filePath, GrammarChecker checker)
			throws IOException {
		if (celeryGrammar == null) {
			try {
				celeryGrammar = NameSpace.loadGrammarFile("celery.nez");
			} catch (IOException e) {
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
		NameSpace grammar = NameSpace.newNameSpace(filePath);
		converter.convert(node, grammar);
		checker.verify(grammar);
		return grammar;
	}

	private void loadPredefinedRules(CommonTree node) {
		JSONPredefinedRules preRules = new JSONPredefinedRules(grammar, rootClassName);
		preRules.defineRule();
	}

	private void convert(CommonTree node, NameSpace grammar) {
		this.grammar = grammar;
		loadPredefinedRules(node);
		this.visit("visit", node);
	}

	// visitor methods

	public void visitRoot(CommonTree node) {
		correctClassNames(node);
		for (CommonTree classNode : node) {
			initMemberList();
			this.visit("visit", classNode);
			String className = classNode.textAt(0, null);
			grammar.defineProduction(classNode, className, genClassRule(className));
		}
		grammar.defineProduction(node, rootClassName, genRootClass());
	}

	public void visitClass(CommonTree node) {
		for (CommonTree memberNode : node) {
			this.visit("visit", memberNode);
		}
	}

	public void visitRequired(CommonTree node) {
		String name = node.textAt(0, null);
		requiredMembersList.add(name);
		Expression[] seq = {
				grammar.newByteChar('"'),
				grammar.newString(name),
				grammar.newByteChar('"'),
				grammar.newNonTerminal("NAMESEP"),
				toExpression(node.get(1)),
				grammar.newOption(grammar.newNonTerminal("VALUESEP"))
		};
		grammar.defineProduction(node, node.textAt(0, null), grammar.newSequence(seq));
	}

	public void visitOption(CommonTree node) {
		String name = node.textAt(0, null);
		impliedMemebersList.add(name);
		Expression[] seq = {
				grammar.newByteChar('"'),
				grammar.newString(name),
				grammar.newByteChar('"'),
				grammar.newNonTerminal("NAMESEP"),
				toExpression(node.get(1)),
				grammar.newOption(grammar.newNonTerminal("VALUESEP"))
		};
		grammar.defineProduction(node, node.textAt(0, null), grammar.newSequence(seq));
	}

	public void visitUntypedRequired(CommonTree node) {
		String name = node.textAt(0, null);
		requiredMembersList.add(name);
		// inferType(node.get(2));
		Expression[] seq = {
				grammar.newByteChar('"'),
				grammar.newString(name),
				grammar.newByteChar('"'),
				grammar.newNonTerminal("NAMESEP"),
				grammar.newNonTerminal("Any"),
				grammar.newOption(grammar.newNonTerminal("VALUESEP"))
		};
		grammar.defineProduction(node, node.textAt(0, null), grammar.newSequence(seq));
	}

	public void visitUntypedOption(CommonTree node) {
		String name = node.textAt(0, null);
		impliedMemebersList.add(name);
		// inferType(node.get(2));
		Expression[] seq = {
				grammar.newByteChar('"'),
				grammar.newString(name),
				grammar.newByteChar('"'),
				grammar.newNonTerminal("NAMESEP"),
				grammar.newNonTerminal("Any"),
				grammar.newOption(grammar.newNonTerminal("VALUESEP"))
		};
		grammar.defineProduction(node, node.textAt(0, null), grammar.newSequence(seq));
	}

	public void visitName(CommonTree node) {
	}

	public Expression visitTEnum(CommonTree node) {
		Expression[] choice = new Expression[node.size()];
		for (int index = 0; index < choice.length; index++) {
			choice[index] = grammar.newString(node.textAt(index, null));
		}
		return grammar.newChoice(choice);
	}

	// to Expression Methods

	private Expression toExpression(CommonTree node) {
		return (Expression) this.visit("to", node);
	}

	public Expression toTBoolean(CommonTree node) {
		return grammar.newNonTerminal("BOOLEAN");
	}

	public Expression toTInteger(CommonTree node) {
		return grammar.newNonTerminal("INT");
	}

	public Expression toTFloat(CommonTree node) {
		return grammar.newNonTerminal("FLOAT");
	}

	public Expression toTString(CommonTree node) {
		return grammar.newNonTerminal("STRING");
	}

	public Expression toTAny(CommonTree node) {
		return grammar.newNonTerminal("Any");
	}

	public Expression toTObject(CommonTree node) {
		return grammar.newNonTerminal("JSONObject");
	}

	public Expression toTClass(CommonTree node) {
		return grammar.newNonTerminal(node.getText());
	}

	public Expression toTArray(CommonTree node) {
		Expression type = toExpression(node.get(0));
		Expression[] seq = {
				grammar.newByteChar('['),
				grammar.newNonTerminal("SPACING"),
				type,
				grammar.newRepetition(grammar.newNonTerminal("VALUESEP"), type),
				grammar.newByteChar(']')
		};
		return grammar.newSequence(seq);
	}

	// generator methods

	private Expression genClassRule(String className) {
		String memberList = className + "_Members";
		Expression[] seq = {
				grammar.newByteChar('"'),
				grammar.newString(className),
				grammar.newByteChar('"'),
				grammar.newNonTerminal("NAMESEP"),
				grammar.newByteChar('{'),
				grammar.newNonTerminal("SPACING"),
				grammar.newNonTerminal(memberList),
				grammar.newNonTerminal("SPACING"),
				grammar.newByteChar('}')
		};
		grammar.defineProduction(null, memberList, genMemberRule(className));
		return grammar.newSequence(seq);
	}

	private Expression genMemberRule(String className) {
		if (impliedMemebersList.isEmpty()) {
			return genCompMember();
		} else {
			String impliedChoiceRuleName = className + "_imp";
			genImpliedChoice(impliedChoiceRuleName);
			return genProxMember(impliedChoiceRuleName);
		}
	}

	private Expression genCompMember() {
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

	private Expression genProxMember(String impliedChoiceRuleName) {
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

	private void genImpliedChoice(String ruleName) {
		Expression[] l = new Expression[impliedMemebersList.size()];
		int choiceCount = 0;
		for (String nonTerminalSymbol : impliedMemebersList) {
			l[choiceCount++] = grammar.newNonTerminal(nonTerminalSymbol);
		}
		grammar.defineProduction(null, ruleName, grammar.newChoice(l));
	}

	private Expression genRootClass() {
		Expression[] root = {
				grammar.newByteChar('{'),
				grammar.newNonTerminal("SPACING"),
				grammar.newNonTerminal(rootClassName + "_Members"),
				grammar.newNonTerminal("SPACING"),
				grammar.newByteChar('}')
		};
		Expression[] seq = {
				grammar.newNonTerminal("VALUESEP"),
				grammar.newSequence(root)
		};
		Expression[] array = {
				grammar.newByteChar('['),
				grammar.newNonTerminal("SPACING"),
				grammar.newSequence(root),
				grammar.newRepetition(seq),
				grammar.newByteChar(']')
		};
		return grammar.newChoice(grammar.newSequence(root), grammar.newSequence(array));
	}

	// Utilities

	private void correctClassNames(CommonTree node) {
		int index = 0;
		for (CommonTree classNode : node) {
			String className = classNode.textAt(0, null);
			classNameMap.put(className, index++);
		}
	}

	private void setRootClassName(String filePath) {
		int offset = filePath.lastIndexOf('/');
		int end = filePath.indexOf('.');
		this.rootClassName = filePath.substring(offset + 1, end);
	}

	private void initMemberList() {
		requiredMembersList = new ArrayList<String>();
		impliedMemebersList = new ArrayList<String>();
	}

	private int[][] permute(int listLength) {
		int[] target = new int[listLength];
		for (int i = 0; i < target.length; i++) {
			target[i] = i;
		}
		PermutationGen permGen = new PermutationGen(target);
		return permGen.getPermList();
	}

}
