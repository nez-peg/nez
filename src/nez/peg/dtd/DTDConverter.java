package nez.peg.dtd;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nez.Parser;
import nez.ParserException;
import nez.Strategy;
import nez.ast.Symbol;
import nez.ast.Tree;
import nez.ast.TreeVisitor;
import nez.io.SourceContext;
import nez.lang.Expression;
import nez.lang.GrammarFile;
import nez.lang.GrammarFileLoader;
import nez.lang.expr.ExpressionCommons;
import nez.util.ConsoleUtils;
import nez.util.UList;

public class DTDConverter extends TreeVisitor {

	static GrammarFile dtdGrammar = null;

	public final static GrammarFile loadGrammar(String filePath, Strategy option) throws IOException {
		option.setEnabled("Wnotice", false);
		if (dtdGrammar == null) {
			try {
				dtdGrammar = (GrammarFile) GrammarFileLoader.loadGrammarFile("xmldtd.nez", null);
			} catch (IOException e) {
				ConsoleUtils.exit(1, "can't load xmldtd.nez");
			}
		}
		Parser p = dtdGrammar.newParser("File");
		SourceContext dtdFile = SourceContext.newFileContext(filePath);
		Tree<?> node = p.parseCommonTree(dtdFile);
		if (node == null) {
			throw new ParserException(dtdFile.getSyntaxErrorMessage());
		}
		if (dtdFile.hasUnconsumed()) {
			throw new ParserException(dtdFile.getUnconsumedMessage());
		}
		DTDConverter conv = new DTDConverter(!option.isEnabled("peg", Strategy.PEG));
		GrammarFile gfile = GrammarFile.newGrammarFile(filePath, option);
		conv.convert(node, gfile);
		return gfile;
	}

	private GrammarFile gfile;
	private boolean enableNezExtension = false;

	DTDConverter() {
	}

	DTDConverter(boolean enableExtension) {
		this.enableNezExtension = enableExtension;
	}

	final void convert(Tree<?> node, GrammarFile grammar) {
		this.gfile = grammar;
		this.loadPredfinedRules(node);
		this.visit("visit", node);
	}

	final void loadPredfinedRules(Tree<?> node) {
		String rootElement = node.get(0).getText(0, null);
		PredefinedRules preRules = new PredefinedRules(this.gfile, rootElement);
		preRules.defineRule();
	}

	int currentElementID;
	int attDefCount = 0;
	int elementCount = 0;
	int entityCount = 0;

	Map<Integer, String> elementNameMap = new HashMap<Integer, String>();
	Map<Integer, String> attDefMap;
	List<Boolean> containsAttributeList = new ArrayList<Boolean>();
	List<Integer> requiredAttList;
	List<Integer> impliedAttList;

	final void initAttCounter() {
		currentElementID = elementCount - 1;
		attDefCount = 0;
		attDefMap = new HashMap<Integer, String>();
		requiredAttList = new ArrayList<Integer>();
		impliedAttList = new ArrayList<Integer>();
	}

	final int[] initAttDefList() {
		int[] attDefList = new int[attDefCount];
		for (int i = 0; i < attDefList.length; i++) {
			attDefList[i] = i;
		}
		return attDefList;
	}

	public void visitDoc(Tree<?> node) {
		for (Tree<?> subnode : node) {
			this.visit("visit", subnode);
		}
		for (int elementID = 0; elementID < elementCount; elementID++) {
			String elementName = "Element_" + elementNameMap.get(elementID);
			gfile.addProduction(node, elementName, genElement(node, elementID));
		}
		gfile.addProduction(node, "Entity", genEntityList(node));
	}

	public void visitElement(Tree<?> node) {
		String elementName = node.getText(0, "");
		elementNameMap.put(elementCount, elementName);
		containsAttributeList.add(false);
		gfile.addProduction(node, "Content" + elementCount, toExpression(node.get(1)));
		elementCount++;
	}

	private Expression genElement(Tree<?> node, int elementID) {
		String elementName = elementNameMap.get(elementID);
		Expression[] contentSeq = { gfile.newByteChar('>'), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), ExpressionCommons.newNonTerminal(null, gfile, "Content" + elementID),
				gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), gfile.newString("</" + elementName + ">"), };
		Expression[] attOnlySeq = { gfile.newString("/>"), };
		Expression[] endChoice = { gfile.newSequence(attOnlySeq), gfile.newSequence(contentSeq) };
		// check whether attribute exists
		if (hasAttribute(elementID)) {
			Expression[] l = { gfile.newString("<" + elementName), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), ExpressionCommons.newNonTerminal(null, gfile, "Attribute" + elementID),
					gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), gfile.newChoice(endChoice), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), };
			return gfile.newSequence(l);
		} else {
			Expression[] l = { gfile.newString("<" + elementName), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), gfile.newChoice(endChoice), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")),

			};
			return gfile.newSequence(l);
		}
	}

	public void visitAttlist(Tree<?> node) {
		initAttCounter();
		containsAttributeList.add(currentElementID, true);
		String attListName = "Attribute" + currentElementID;
		String choiceListName = "AttChoice" + currentElementID;

		for (int attNum = 1; attNum < node.size(); attNum++) {
			this.visit("visit", node.get(attNum));
		}

		int[] attDefList = initAttDefList();
		int[] requiredRules = extractRequiredRule(attDefList);

		// generate Complete / Proximate Attribute list
		if (enableNezExtension) {
			gfile.addProduction(node, attListName, genExAtt(node, requiredRules));
		} else {
			if (impliedAttList.isEmpty()) {
				gfile.addProduction(node, attListName, genCompAtt(node, attDefList));
			} else {
				gfile.addProduction(node, choiceListName, genImpliedChoice(node));
				gfile.addProduction(node, attListName, genProxAtt(node, requiredRules));
			}
		}
	}

	public void visitREQUIRED(Tree<?> node) {
		String name = "AttDef" + currentElementID + "_" + attDefCount;
		attDefMap.put(attDefCount, node.getText(0, ""));
		requiredAttList.add(attDefCount);
		gfile.addProduction(node, name, toExpression(node.get(1)));
		attDefCount++;
	}

	public void visitIMPLIED(Tree<?> node) {
		String name = "AttDef" + currentElementID + "_" + attDefCount;
		attDefMap.put(attDefCount, node.getText(0, ""));
		impliedAttList.add(attDefCount);
		gfile.addProduction(node, name, toExpression(node.get(1)));
		attDefCount++;
	}

	public void visitFIXED(Tree<?> node) {
		String name = "AttDef" + currentElementID + "_" + attDefCount;
		attDefMap.put(attDefCount, node.getText(0, ""));
		impliedAttList.add(attDefCount);
		gfile.addProduction(node, name, genFixedAtt(node));
		attDefCount++;
	}

	public void visitDefault(Tree<?> node) {
		String name = "AttDef" + currentElementID + "_" + attDefCount;
		attDefMap.put(attDefCount, node.getText(0, ""));
		impliedAttList.add(attDefCount);
		gfile.addProduction(node, name, toExpression(node.get(1)));
		attDefCount++;
	}

	public void visitEntity(Tree<?> node) {
		String name = "ENT_" + entityCount++;
		gfile.addProduction(node, name, toExpression(node.get(1)));
	}

	private Expression toExpression(Tree<?> node) {
		return (Expression) this.visit("to", node);
	}

	public Expression toEmpty(Tree<?> node) {
		return ExpressionCommons.newNonTerminal(null, gfile, "EMPTY");
	}

	public Expression toAny(Tree<?> node) {
		return ExpressionCommons.newNonTerminal(null, gfile, "ANY");
	}

	public Expression toZeroMore(Tree<?> node) {
		return gfile.newRepetition(toExpression(node.get(0)));
	}

	public Expression toOneMore(Tree<?> node) {
		return gfile.newRepetition1(toExpression(node.get(0)));
	}

	public Expression toOption(Tree<?> node) {
		return gfile.newOption(toExpression(node.get(0)));
	}

	public Expression toChoice(Tree<?> node) {
		Expression[] l = new Expression[node.size()];
		int count = 0;
		for (Tree<?> subnode : node) {
			l[count++] = toExpression(subnode);
		}
		return gfile.newChoice(l);
	}

	public Expression toSeq(Tree<?> node) {
		Expression[] l = new Expression[node.size()];
		int count = 0;
		for (Tree<?> subnode : node) {
			l[count++] = toExpression(subnode);
		}
		return gfile.newSequence(l);
	}

	public Expression toCDATA(Tree<?> node) {
		if (enableNezExtension) {
			return _AttDef("STRING");
		}
		return _Att("STRING");
	}

	public Expression toID(Tree<?> node) {
		if (enableNezExtension) {
			return _AttDefQ("IDTOKEN");
		}
		return _AttQ("IDTOKEN");
	}

	public Expression toIDREF(Tree<?> node) {
		if (enableNezExtension) {
			return _AttDefQ("IDTOKEN");
		}
		return _AttQ("IDTOKEN");
	}

	public Expression toIDREFS(Tree<?> node) {
		if (enableNezExtension) {
			return _AttDefQ("IDTOKENS");
		}
		return _AttQ("IDTOKENS");
	}

	public Expression toENTITY(Tree<?> node) {
		if (enableNezExtension) {
			return _AttDefQ("entity");
		}
		return _AttQ("entity");
	}

	public Expression toENTITIES(Tree<?> node) {
		if (enableNezExtension) {
			return _AttDefQ("entities");
		}
		return _AttQ("entities");
	}

	public Expression toNMTOKEN(Tree<?> node) {
		if (enableNezExtension) {
			return _AttDef("NMTOKEN");
		}
		return _Att("NMTOKEN");
	}

	public Expression toNMTOKENS(Tree<?> node) {
		if (enableNezExtension) {
			return _AttDef("NMTOKENS");
		}
		return _Att("NMTOKENS");
	}

	public Expression _Att(String type) {
		String attName = attDefMap.get(attDefCount);
		Expression[] l = { gfile.newString(attName), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), gfile.newByteChar('='), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")),
				ExpressionCommons.newNonTerminal(null, gfile, type), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), };
		return gfile.newSequence(l);
	}

	public Expression _AttQ(String type) {
		String attName = attDefMap.get(attDefCount);
		Expression[] l = { gfile.newString(attName), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), gfile.newByteChar('='), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), gfile.newString("\""),
				ExpressionCommons.newNonTerminal(null, gfile, type), gfile.newString("\""), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), };
		return gfile.newSequence(l);
	}

	public Expression _AttDef(String type) {
		String attName = attDefMap.get(attDefCount);
		Expression[] l = { ExpressionCommons.newXdef(null, gfile, "T" + currentElementID, gfile.newString(attName)), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), gfile.newByteChar('='),
				gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), ExpressionCommons.newNonTerminal(null, gfile, type), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), };
		return gfile.newSequence(l);
	}

	public Expression _AttDefQ(String type) {
		String attName = attDefMap.get(attDefCount);
		Expression[] l = { ExpressionCommons.newXdef(null, gfile, "T" + currentElementID, gfile.newString(attName)), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), gfile.newByteChar('='),
				gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), gfile.newString("\""), ExpressionCommons.newNonTerminal(null, gfile, type), gfile.newString("\""),
				gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), };
		return gfile.newSequence(l);
	}

	private Expression genFixedAtt(Tree<?> node) {
		String attName = attDefMap.get(attDefCount);
		String fixedValue = "\"" + node.getText(2, "") + "\"";
		Expression[] l = { gfile.newString(attName), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), gfile.newByteChar('='), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), gfile.newString(fixedValue),
				gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")),

		};
		return gfile.newSequence(l);
	}

	public Expression genExAtt(Tree<?> node, int[] requiredList) {
		Symbol tableName = Symbol.tag("T" + currentElementID);
		String attDefList = "AttDefList" + currentElementID;
		gfile.addProduction(node, attDefList, genExAttDefList(node, requiredList, tableName));
		UList<Expression> seq = new UList<Expression>(new Expression[requiredList.length + 1]);
		seq.add(ExpressionCommons.newPzero(node, ExpressionCommons.newNonTerminal(node, gfile, attDefList)));
		for (int index : requiredList) {
			seq.add(ExpressionCommons.newXexists(node, tableName, attDefMap.get(index)));
		}
		return ExpressionCommons.newXlocal(node, tableName, ExpressionCommons.newPsequence(node, seq));
	}

	public Expression genExAttDefList(Tree<?> node, int[] requiredList, Symbol tableName) {
		UList<Expression> l = new UList<Expression>(new Expression[requiredList.length + 1]);
		for (int index : requiredList) {
			Expression notexist = ExpressionCommons.newPnot(node, ExpressionCommons.newXexists(node, tableName, attDefMap.get(index)));
			Expression nonterminal = ExpressionCommons.newNonTerminal(node, gfile, "AttDef" + currentElementID + "_" + index);
			l.add(ExpressionCommons.newPsequence(node, notexist, nonterminal));
		}
		if (!impliedAttList.isEmpty()) {
			String choiceListName = "AttChoice" + currentElementID;
			gfile.addProduction(node, choiceListName, genImpliedChoice(node));
			l.add(ExpressionCommons.newNonTerminal(node, gfile, choiceListName));
		}
		return ExpressionCommons.newPchoice(node, l);
	}

	public Expression genCompAtt(Tree<?> node, int[] attlist) {
		int listLength = attlist.length;
		if (listLength == 1) {
			Expression[] l = { ExpressionCommons.newNonTerminal(null, gfile, "AttDef" + currentElementID + "_" + attlist[0]), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), ExpressionCommons.newNonTerminal(null, gfile, "ENDTAG") };
			return gfile.newSequence(l);
		} else {
			int[][] permutationList = perm(attlist);
			Expression[] choiceList = new Expression[permutationList.length];
			int choiceCount = 0;
			for (int[] target : permutationList) {
				Expression[] seqList = new Expression[listLength + 1];
				for (int index = 0; index < target.length; index++) {
					seqList[index] = ExpressionCommons.newNonTerminal(null, gfile, "AttDef" + currentElementID + "_" + target[index]);
				}
				seqList[listLength] = ExpressionCommons.newNonTerminal(null, gfile, "ENDTAG");
				choiceList[choiceCount++] = gfile.newSequence(seqList);
			}
			return gfile.newChoice(choiceList);
		}
	}

	public Expression genProxAtt(Tree<?> node, int[] attlist) {
		int listLength = attlist.length;
		if (listLength == 0) {
			Expression[] l = { gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "AttChoice" + currentElementID)), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")),
					ExpressionCommons.newNonTerminal(null, gfile, "ENDTAG") };
			return gfile.newSequence(l);
		} else {
			int[][] permedList = perm(attlist);
			Expression[] choiceList = new Expression[permedList.length];
			int choiceCount = 0;
			for (int[] target : permedList) {
				Expression[] seqList = new Expression[listLength * 2 + 2];
				int seqCount = 0;
				seqList[seqCount++] = gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "AttChoice" + currentElementID));
				for (int index = 0; index < target.length; index++) {
					seqList[seqCount++] = ExpressionCommons.newNonTerminal(null, gfile, "AttDef" + currentElementID + "_" + target[index]);
					seqList[seqCount++] = gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "AttChoice" + currentElementID));
				}
				seqList[seqCount] = ExpressionCommons.newNonTerminal(null, gfile, "ENDTAG");
				choiceList[choiceCount++] = gfile.newSequence(seqList);
			}
			return gfile.newChoice(choiceList);
		}
	}

	public Expression genImpliedChoice(Tree<?> node) {
		Expression[] l = new Expression[impliedAttList.size()];
		String definitionName = "AttDef" + currentElementID + "_";
		int choiceCount = 0;
		for (Integer ruleNum : impliedAttList) {
			l[choiceCount++] = ExpressionCommons.newNonTerminal(null, gfile, definitionName + ruleNum);
		}
		return gfile.newChoice(l);
	}

	public Expression toEnum(Tree<?> node) {
		String attName = attDefMap.get(attDefCount);
		Expression[] l = { gfile.newString(attName), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), gfile.newByteChar('='), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), gfile.newByteChar('"'),
				toChoice(node), gfile.newByteChar('"'), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), };
		return gfile.newSequence(l);
	}

	public Expression toEntValue(Tree<?> node) {
		String replaceString = node.toText();
		return gfile.newString(replaceString);
	}

	public Expression toElName(Tree<?> node) {
		String elementName = "Element_" + node.toText();
		return ExpressionCommons.newNonTerminal(null, gfile, elementName);
	}

	public Expression toName(Tree<?> node) {
		return gfile.newString(node.toText());
	}

	public Expression toData(Tree<?> node) {
		return ExpressionCommons.newNonTerminal(null, gfile, "PCDATA");
	}

	public Expression toOnlyData(Tree<?> node) {
		return gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "PCDATA"));
	}

	private Expression genEntityList(Tree<?> node) {
		if (entityCount == 0) {
			return ExpressionCommons.newFailure(null);
		} else {
			Expression[] l = new Expression[entityCount];
			for (int entityNum = 0; entityNum < entityCount; entityNum++) {
				l[entityNum] = ExpressionCommons.newNonTerminal(null, gfile, "ENT_" + entityNum);
			}
			return gfile.newChoice(l);
		}
	}

	private final int[] extractRequiredRule(int[] attlist) {
		int[] buf = new int[512];
		int arrIndex = 0;
		for (int requiredNum : attlist) {
			if (requiredAttList.contains(requiredNum)) {
				buf[arrIndex++] = requiredNum;
			}
		}
		int[] target = new int[arrIndex];
		for (int i = 0; i < arrIndex; i++) {
			target[i] = buf[i];
		}
		return target;
	}

	private final int[][] perm(int[] attlist) {
		Permutation permutation = new Permutation(attlist);
		return permutation.getPermList();
	}

	private final boolean hasAttribute(int elementID) {
		return containsAttributeList.get(elementID);
	}

}
