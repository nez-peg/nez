package nez.peg.dtd;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nez.Parser;
import nez.NezException;
import nez.NezOption;
import nez.SourceContext;
import nez.ast.AbstractTree;
import nez.ast.AbstractTreeVisitor;
import nez.ast.SymbolId;
import nez.lang.Expression;
import nez.lang.GrammarFile;
import nez.lang.expr.ExpressionCommons;
import nez.util.ConsoleUtils;
import nez.util.UList;

public class DTDConverter extends AbstractTreeVisitor {

	static GrammarFile dtdGrammar = null;

	public final static GrammarFile loadGrammar(String filePath, NezOption option) throws IOException {
		option.setOption("notice", false);
		if (dtdGrammar == null) {
			try {
				dtdGrammar = GrammarFile.loadGrammarFile("xmldtd.nez", NezOption.newSafeOption());
			} catch (IOException e) {
				ConsoleUtils.exit(1, "can't load xmldtd.nez");
			}
		}
		Parser p = dtdGrammar.newGrammar("File");
		SourceContext dtdFile = SourceContext.newFileContext(filePath);
		AbstractTree<?> node = p.parseCommonTree(dtdFile);
		if (node == null) {
			throw new NezException(dtdFile.getSyntaxErrorMessage());
		}
		if (dtdFile.hasUnconsumed()) {
			throw new NezException(dtdFile.getUnconsumedMessage());
		}
		DTDConverter conv = new DTDConverter(!option.disabledNezExtension);
		GrammarFile gfile = GrammarFile.newGrammarFile(filePath, option);
		conv.convert(node, gfile);
		gfile.verify();
		return gfile;
	}

	private GrammarFile gfile;
	private boolean enableNezExtension = false;

	DTDConverter() {
	}

	DTDConverter(boolean enableExtension) {
		this.enableNezExtension = enableExtension;
	}

	final void convert(AbstractTree<?> node, GrammarFile grammar) {
		this.gfile = grammar;
		this.loadPredfinedRules(node);
		this.visit("visit", node);
	}

	final void loadPredfinedRules(AbstractTree<?> node) {
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

	public void visitDoc(AbstractTree<?> node) {
		for (AbstractTree<?> subnode : node) {
			this.visit("visit", subnode);
		}
		for (int elementID = 0; elementID < elementCount; elementID++) {
			String elementName = "Element_" + elementNameMap.get(elementID);
			gfile.defineProduction(node, elementName, genElement(node, elementID));
		}
		gfile.defineProduction(node, "Entity", genEntityList(node));
	}

	public void visitElement(AbstractTree<?> node) {
		String elementName = node.getText(0, "");
		elementNameMap.put(elementCount, elementName);
		containsAttributeList.add(false);
		gfile.defineProduction(node, "Content" + elementCount, toExpression(node.get(1)));
		elementCount++;
	}

	private Expression genElement(AbstractTree<?> node, int elementID) {
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

	public void visitAttlist(AbstractTree<?> node) {
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
			gfile.defineProduction(node, attListName, genExAtt(node, requiredRules));
		} else {
			if (impliedAttList.isEmpty()) {
				gfile.defineProduction(node, attListName, genCompAtt(node, attDefList));
			} else {
				gfile.defineProduction(node, choiceListName, genImpliedChoice(node));
				gfile.defineProduction(node, attListName, genProxAtt(node, requiredRules));
			}
		}
	}

	public void visitREQUIRED(AbstractTree<?> node) {
		String name = "AttDef" + currentElementID + "_" + attDefCount;
		attDefMap.put(attDefCount, node.getText(0, ""));
		requiredAttList.add(attDefCount);
		gfile.defineProduction(node, name, toExpression(node.get(1)));
		attDefCount++;
	}

	public void visitIMPLIED(AbstractTree<?> node) {
		String name = "AttDef" + currentElementID + "_" + attDefCount;
		attDefMap.put(attDefCount, node.getText(0, ""));
		impliedAttList.add(attDefCount);
		gfile.defineProduction(node, name, toExpression(node.get(1)));
		attDefCount++;
	}

	public void visitFIXED(AbstractTree<?> node) {
		String name = "AttDef" + currentElementID + "_" + attDefCount;
		attDefMap.put(attDefCount, node.getText(0, ""));
		impliedAttList.add(attDefCount);
		gfile.defineProduction(node, name, genFixedAtt(node));
		attDefCount++;
	}

	public void visitDefault(AbstractTree<?> node) {
		String name = "AttDef" + currentElementID + "_" + attDefCount;
		attDefMap.put(attDefCount, node.getText(0, ""));
		impliedAttList.add(attDefCount);
		gfile.defineProduction(node, name, toExpression(node.get(1)));
		attDefCount++;
	}

	public void visitEntity(AbstractTree<?> node) {
		String name = "ENT_" + entityCount++;
		gfile.defineProduction(node, name, toExpression(node.get(1)));
	}

	private Expression toExpression(AbstractTree<?> node) {
		return (Expression) this.visit("to", node);
	}

	public Expression toEmpty(AbstractTree<?> node) {
		return ExpressionCommons.newNonTerminal(null, gfile, "EMPTY");
	}

	public Expression toAny(AbstractTree<?> node) {
		return ExpressionCommons.newNonTerminal(null, gfile, "ANY");
	}

	public Expression toZeroMore(AbstractTree<?> node) {
		return gfile.newRepetition(toExpression(node.get(0)));
	}

	public Expression toOneMore(AbstractTree<?> node) {
		return gfile.newRepetition1(toExpression(node.get(0)));
	}

	public Expression toOption(AbstractTree<?> node) {
		return gfile.newOption(toExpression(node.get(0)));
	}

	public Expression toChoice(AbstractTree<?> node) {
		Expression[] l = new Expression[node.size()];
		int count = 0;
		for (AbstractTree<?> subnode : node) {
			l[count++] = toExpression(subnode);
		}
		return gfile.newChoice(l);
	}

	public Expression toSeq(AbstractTree<?> node) {
		Expression[] l = new Expression[node.size()];
		int count = 0;
		for (AbstractTree<?> subnode : node) {
			l[count++] = toExpression(subnode);
		}
		return gfile.newSequence(l);
	}

	public Expression toCDATA(AbstractTree<?> node) {
		if (enableNezExtension) {
			return _AttDef("STRING");
		}
		return _Att("STRING");
	}

	public Expression toID(AbstractTree<?> node) {
		if (enableNezExtension) {
			return _AttDefQ("IDTOKEN");
		}
		return _AttQ("IDTOKEN");
	}

	public Expression toIDREF(AbstractTree<?> node) {
		if (enableNezExtension) {
			return _AttDefQ("IDTOKEN");
		}
		return _AttQ("IDTOKEN");
	}

	public Expression toIDREFS(AbstractTree<?> node) {
		if (enableNezExtension) {
			return _AttDefQ("IDTOKENS");
		}
		return _AttQ("IDTOKENS");
	}

	public Expression toENTITY(AbstractTree<?> node) {
		if (enableNezExtension) {
			return _AttDefQ("entity");
		}
		return _AttQ("entity");
	}

	public Expression toENTITIES(AbstractTree<?> node) {
		if (enableNezExtension) {
			return _AttDefQ("entities");
		}
		return _AttQ("entities");
	}

	public Expression toNMTOKEN(AbstractTree<?> node) {
		if (enableNezExtension) {
			return _AttDef("NMTOKEN");
		}
		return _Att("NMTOKEN");
	}

	public Expression toNMTOKENS(AbstractTree<?> node) {
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
		Expression[] l = { ExpressionCommons.newXdef(null, gfile, SymbolId.tag("T" + currentElementID), gfile.newString(attName)), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), gfile.newByteChar('='),
				gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), ExpressionCommons.newNonTerminal(null, gfile, type), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), };
		return gfile.newSequence(l);
	}

	public Expression _AttDefQ(String type) {
		String attName = attDefMap.get(attDefCount);
		Expression[] l = { ExpressionCommons.newXdef(null, gfile, SymbolId.tag("T" + currentElementID), gfile.newString(attName)), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), gfile.newByteChar('='),
				gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), gfile.newString("\""), ExpressionCommons.newNonTerminal(null, gfile, type), gfile.newString("\""), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), };
		return gfile.newSequence(l);
	}

	private Expression genFixedAtt(AbstractTree<?> node) {
		String attName = attDefMap.get(attDefCount);
		String fixedValue = "\"" + node.getText(2, "") + "\"";
		Expression[] l = { gfile.newString(attName), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), gfile.newByteChar('='), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), gfile.newString(fixedValue),
				gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")),

		};
		return gfile.newSequence(l);
	}

	public Expression genExAtt(AbstractTree<?> node, int[] requiredList) {
		SymbolId tableName = SymbolId.tag("T" + currentElementID);
		String attDefList = "AttDefList" + currentElementID;
		gfile.defineProduction(node, attDefList, genExAttDefList(node, requiredList, tableName));
		UList<Expression> seq = new UList<Expression>(new Expression[requiredList.length + 1]);
		seq.add(ExpressionCommons.newUzero(node, ExpressionCommons.newNonTerminal(node, gfile, attDefList)));
		for (int index : requiredList) {
			seq.add(ExpressionCommons.newXexists(node, tableName, attDefMap.get(index)));
		}
		return ExpressionCommons.newXlocal(node, tableName, ExpressionCommons.newPsequence(node, seq));
	}

	public Expression genExAttDefList(AbstractTree<?> node, int[] requiredList, SymbolId tableName) {
		UList<Expression> l = new UList<Expression>(new Expression[requiredList.length + 1]);
		for (int index : requiredList) {
			Expression notexist = ExpressionCommons.newUnot(node, ExpressionCommons.newXexists(node, tableName, attDefMap.get(index)));
			Expression nonterminal = ExpressionCommons.newNonTerminal(node, gfile, "AttDef" + currentElementID + "_" + index);
			l.add(ExpressionCommons.newPsequence(node, notexist, nonterminal));
		}
		if (!impliedAttList.isEmpty()) {
			String choiceListName = "AttChoice" + currentElementID;
			gfile.defineProduction(node, choiceListName, genImpliedChoice(node));
			l.add(ExpressionCommons.newNonTerminal(node, gfile, choiceListName));
		}
		return ExpressionCommons.newPchoice(node, l);
	}

	public Expression genCompAtt(AbstractTree<?> node, int[] attlist) {
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

	public Expression genProxAtt(AbstractTree<?> node, int[] attlist) {
		int listLength = attlist.length;
		if (listLength == 0) {
			Expression[] l = { gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "AttChoice" + currentElementID)), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), ExpressionCommons.newNonTerminal(null, gfile, "ENDTAG") };
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

	public Expression genImpliedChoice(AbstractTree<?> node) {
		Expression[] l = new Expression[impliedAttList.size()];
		String definitionName = "AttDef" + currentElementID + "_";
		int choiceCount = 0;
		for (Integer ruleNum : impliedAttList) {
			l[choiceCount++] = ExpressionCommons.newNonTerminal(null, gfile, definitionName + ruleNum);
		}
		return gfile.newChoice(l);
	}

	public Expression toEnum(AbstractTree<?> node) {
		String attName = attDefMap.get(attDefCount);
		Expression[] l = { gfile.newString(attName), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), gfile.newByteChar('='), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), gfile.newByteChar('"'),
				toChoice(node), gfile.newByteChar('"'), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), };
		return gfile.newSequence(l);
	}

	public Expression toEntValue(AbstractTree<?> node) {
		String replaceString = node.toText();
		return gfile.newString(replaceString);
	}

	public Expression toElName(AbstractTree<?> node) {
		String elementName = "Element_" + node.toText();
		return ExpressionCommons.newNonTerminal(null, gfile, elementName);
	}

	public Expression toName(AbstractTree<?> node) {
		return gfile.newString(node.toText());
	}

	public Expression toData(AbstractTree<?> node) {
		return ExpressionCommons.newNonTerminal(null, gfile, "PCDATA");
	}

	public Expression toOnlyData(AbstractTree<?> node) {
		return gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "PCDATA"));
	}

	private Expression genEntityList(AbstractTree<?> node) {
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
