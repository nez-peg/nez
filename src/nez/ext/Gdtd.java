package nez.ext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nez.Grammar;
import nez.Parser;
import nez.Strategy;
import nez.ast.Symbol;
import nez.ast.Tree;
import nez.lang.Expression;
import nez.lang.GrammarFileLoader;
import nez.lang.expr.ExpressionCommons;
import nez.util.ConsoleUtils;
import nez.util.UList;

public class Gdtd extends GrammarFileLoader {

	public Gdtd() {
	}

	static Parser dtdParser;
	boolean enableNezExtension;

	@Override
	public Parser getLoaderParser(String start) {
		if (dtdParser == null) {
			try {
				Strategy option = Strategy.newSafeStrategy();
				Grammar g = GrammarFileLoader.loadGrammar("xmldtd.nez", option);
				dtdParser = g.newParser(option);
				strategy.report();
			} catch (IOException e) {
				ConsoleUtils.exit(1, "unload: " + e.getMessage());
			}
			assert (dtdParser != null);
		}
		this.enableNezExtension = !strategy.isEnabled("peg", Strategy.PEG);
		return dtdParser;
	}

	private final void loadPredefinedRules(Tree<?> node) {
		String rootElement = node.get(0).getText(0, null);
		PredefinedRules preRules = new PredefinedRules(getGrammar(), rootElement);
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

	@Override
	public void parse(Tree<?> node) {
		loadPredefinedRules(node);
		visit("visit", node);
	}

	public void visitDoc(Tree<?> node) {
		Grammar gfile = getGrammar();
		for (Tree<?> subnode : node) {
			this.visit("visit", subnode);
		}
		for (int elementID = 0; elementID < elementCount; elementID++) {
			String elementName = "Element_" + elementNameMap.get(elementID);
			gfile.newProduction(node, 0, elementName, genElement(node, elementID));
		}
		gfile.newProduction(node, 0, "Entity", genEntityList(node));
	}

	public void visitElement(Tree<?> node) {
		String elementName = node.getText(0, "");
		elementNameMap.put(elementCount, elementName);
		containsAttributeList.add(false);
		getGrammar().newProduction(node, 0, "Content" + elementCount, toExpression(node.get(1)));
		elementCount++;
	}

	private Expression genElement(Tree<?> node, int elementID) {
		Grammar gfile = getGrammar();
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
		Grammar gfile = getGrammar();
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
			gfile.newProduction(node, 0, attListName, genExAtt(node, requiredRules));
		} else {
			if (impliedAttList.isEmpty()) {
				gfile.newProduction(node, 0, attListName, genCompAtt(node, attDefList));
			} else {
				gfile.newProduction(node, 0, choiceListName, genImpliedChoice(node));
				gfile.newProduction(node, 0, attListName, genProxAtt(node, requiredRules));
			}
		}
	}

	public void visitREQUIRED(Tree<?> node) {
		String name = "AttDef" + currentElementID + "_" + attDefCount;
		attDefMap.put(attDefCount, node.getText(0, ""));
		requiredAttList.add(attDefCount);
		getGrammar().newProduction(node, 0, name, toExpression(node.get(1)));
		attDefCount++;
	}

	public void visitIMPLIED(Tree<?> node) {
		String name = "AttDef" + currentElementID + "_" + attDefCount;
		attDefMap.put(attDefCount, node.getText(0, ""));
		impliedAttList.add(attDefCount);
		getGrammar().newProduction(node, 0, name, toExpression(node.get(1)));
		attDefCount++;
	}

	public void visitFIXED(Tree<?> node) {
		String name = "AttDef" + currentElementID + "_" + attDefCount;
		attDefMap.put(attDefCount, node.getText(0, ""));
		impliedAttList.add(attDefCount);
		getGrammar().newProduction(node, 0, name, genFixedAtt(node));
		attDefCount++;
	}

	public void visitDefault(Tree<?> node) {
		String name = "AttDef" + currentElementID + "_" + attDefCount;
		attDefMap.put(attDefCount, node.getText(0, ""));
		impliedAttList.add(attDefCount);
		getGrammar().newProduction(node, 0, name, toExpression(node.get(1)));
		attDefCount++;
	}

	public void visitEntity(Tree<?> node) {
		String name = "ENT_" + entityCount++;
		getGrammar().newProduction(node, 0, name, toExpression(node.get(1)));
	}

	private Expression toExpression(Tree<?> node) {
		return (Expression) this.visit("to", node);
	}

	public Expression toEmpty(Tree<?> node) {
		return ExpressionCommons.newNonTerminal(null, getGrammar(), "EMPTY");
	}

	public Expression toAny(Tree<?> node) {
		return ExpressionCommons.newNonTerminal(null, getGrammar(), "ANY");
	}

	public Expression toZeroMore(Tree<?> node) {
		return getGrammar().newRepetition(toExpression(node.get(0)));
	}

	public Expression toOneMore(Tree<?> node) {
		return getGrammar().newRepetition1(toExpression(node.get(0)));
	}

	public Expression toOption(Tree<?> node) {
		return getGrammar().newOption(toExpression(node.get(0)));
	}

	public Expression toChoice(Tree<?> node) {
		Expression[] l = new Expression[node.size()];
		int count = 0;
		for (Tree<?> subnode : node) {
			l[count++] = toExpression(subnode);
		}
		return getGrammar().newChoice(l);
	}

	public Expression toSeq(Tree<?> node) {
		Expression[] l = new Expression[node.size()];
		int count = 0;
		for (Tree<?> subnode : node) {
			l[count++] = toExpression(subnode);
		}
		return getGrammar().newSequence(l);
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
		Grammar gfile = getGrammar();
		String attName = attDefMap.get(attDefCount);
		Expression[] l = { gfile.newString(attName), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), gfile.newByteChar('='), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")),
				ExpressionCommons.newNonTerminal(null, gfile, type), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), };
		return gfile.newSequence(l);
	}

	public Expression _AttQ(String type) {
		Grammar gfile = getGrammar();
		String attName = attDefMap.get(attDefCount);
		Expression[] l = { gfile.newString(attName), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), gfile.newByteChar('='), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), gfile.newString("\""),
				ExpressionCommons.newNonTerminal(null, gfile, type), gfile.newString("\""), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), };
		return gfile.newSequence(l);
	}

	public Expression _AttDef(String type) {
		Grammar gfile = getGrammar();
		String attName = attDefMap.get(attDefCount);
		Expression[] l = { ExpressionCommons.newXdef(null, gfile, "T" + currentElementID, gfile.newString(attName)), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), gfile.newByteChar('='),
				gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), ExpressionCommons.newNonTerminal(null, gfile, type), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), };
		return gfile.newSequence(l);
	}

	public Expression _AttDefQ(String type) {
		Grammar gfile = getGrammar();
		String attName = attDefMap.get(attDefCount);
		Expression[] l = { ExpressionCommons.newXdef(null, gfile, "T" + currentElementID, gfile.newString(attName)), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), gfile.newByteChar('='),
				gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), gfile.newString("\""), ExpressionCommons.newNonTerminal(null, gfile, type), gfile.newString("\""),
				gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), };
		return gfile.newSequence(l);
	}

	private Expression genFixedAtt(Tree<?> node) {
		Grammar gfile = getGrammar();
		String attName = attDefMap.get(attDefCount);
		String fixedValue = "\"" + node.getText(2, "") + "\"";
		Expression[] l = { gfile.newString(attName), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), gfile.newByteChar('='), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), gfile.newString(fixedValue),
				gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")),

		};
		return gfile.newSequence(l);
	}

	public Expression genExAtt(Tree<?> node, int[] requiredList) {
		Grammar gfile = getGrammar();
		Symbol tableName = Symbol.tag("T" + currentElementID);
		String attDefList = "AttDefList" + currentElementID;
		gfile.newProduction(node, 0, attDefList, genExAttDefList(node, requiredList, tableName));
		UList<Expression> seq = new UList<Expression>(new Expression[requiredList.length + 1]);
		seq.add(ExpressionCommons.newPzero(node, ExpressionCommons.newNonTerminal(node, gfile, attDefList)));
		for (int index : requiredList) {
			seq.add(ExpressionCommons.newXexists(node, tableName, attDefMap.get(index)));
		}
		return ExpressionCommons.newXlocal(node, tableName, ExpressionCommons.newPsequence(node, seq));
	}

	public Expression genExAttDefList(Tree<?> node, int[] requiredList, Symbol tableName) {
		Grammar gfile = getGrammar();
		UList<Expression> l = new UList<Expression>(new Expression[requiredList.length + 1]);
		for (int index : requiredList) {
			Expression notexist = ExpressionCommons.newPnot(node, ExpressionCommons.newXexists(node, tableName, attDefMap.get(index)));
			Expression nonterminal = ExpressionCommons.newNonTerminal(node, gfile, "AttDef" + currentElementID + "_" + index);
			l.add(ExpressionCommons.newPsequence(node, notexist, nonterminal));
		}
		if (!impliedAttList.isEmpty()) {
			String choiceListName = "AttChoice" + currentElementID;
			gfile.newProduction(node, 0, choiceListName, genImpliedChoice(node));
			l.add(ExpressionCommons.newNonTerminal(node, gfile, choiceListName));
		}
		return ExpressionCommons.newPchoice(node, l);
	}

	public Expression genCompAtt(Tree<?> node, int[] attlist) {
		Grammar gfile = getGrammar();
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
		Grammar gfile = getGrammar();
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
		Grammar gfile = getGrammar();
		Expression[] l = new Expression[impliedAttList.size()];
		String definitionName = "AttDef" + currentElementID + "_";
		int choiceCount = 0;
		for (Integer ruleNum : impliedAttList) {
			l[choiceCount++] = ExpressionCommons.newNonTerminal(null, gfile, definitionName + ruleNum);
		}
		return gfile.newChoice(l);
	}

	public Expression toEnum(Tree<?> node) {
		Grammar gfile = getGrammar();
		String attName = attDefMap.get(attDefCount);
		Expression[] l = { gfile.newString(attName), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), gfile.newByteChar('='), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), gfile.newByteChar('"'),
				toChoice(node), gfile.newByteChar('"'), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), };
		return gfile.newSequence(l);
	}

	public Expression toEntValue(Tree<?> node) {
		String replaceString = node.toText();
		return getGrammar().newString(replaceString);
	}

	public Expression toElName(Tree<?> node) {
		String elementName = "Element_" + node.toText();
		return ExpressionCommons.newNonTerminal(null, getGrammar(), elementName);
	}

	public Expression toName(Tree<?> node) {
		return getGrammar().newString(node.toText());
	}

	public Expression toData(Tree<?> node) {
		return ExpressionCommons.newNonTerminal(null, getGrammar(), "PCDATA");
	}

	public Expression toOnlyData(Tree<?> node) {
		return ExpressionCommons.newPzero(null, ExpressionCommons.newNonTerminal(null, getGrammar(), "PCDATA"));
	}

	private Expression genEntityList(Tree<?> node) {
		Grammar gfile = getGrammar();
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

class PredefinedRules {
	Grammar grammar;
	String rootElement;

	public PredefinedRules(Grammar grammar, String rootElement) {
		this.grammar = grammar;
		this.rootElement = rootElement;
	}

	public void defineRule() {
		defToplevel();
		defChunk();
		defFile();
		defDocument();
		defProlog();
		defDoctype();
		defContent();
		defName();
		defString();
		defNameToken();
		defIdToken();
		defText();
		defAttribute();
		defCDATASECT();
		defMISC();
		defCOMMENT();
		defPCDATA();
		defEMPTY();
		defANY();
		defSpacing();
		defENDTAG();
		defNotAny();
		defRootElement();
		defPreEntity();
	}

	final void defToplevel() {
		grammar.newProduction(null, 0, "Toplevel", ExpressionCommons.newNonTerminal(null, grammar, "Document"));

	}

	final void defChunk() {
		grammar.newProduction(null, 0, "Chunk", ExpressionCommons.newNonTerminal(null, grammar, "Document"));
	}

	final void defFile() {
		grammar.newProduction(null, 0, "File", ExpressionCommons.newNonTerminal(null, grammar, "Document"));
	}

	final void defDocument() {
		Expression[] l = { ExpressionCommons.newPoption(null, ExpressionCommons.newNonTerminal(null, grammar, "PROLOG")), ExpressionCommons.newPzero(null, ExpressionCommons.newNonTerminal(null, grammar, "MISC")),
				ExpressionCommons.newPoption(null, ExpressionCommons.newNonTerminal(null, grammar, "DOCTYPE")), ExpressionCommons.newPzero(null, ExpressionCommons.newNonTerminal(null, grammar, "MISC")),
				ExpressionCommons.newNonTerminal(null, grammar, "Content"), ExpressionCommons.newPzero(null, ExpressionCommons.newNonTerminal(null, grammar, "MISC")), };
		grammar.newProduction(null, 0, "Document", grammar.newSequence(l));
	}

	final void defProlog() {
		Expression[] l = { ExpressionCommons.newString(null, "<?xml"), ExpressionCommons.newPzero(null, ExpressionCommons.newNonTerminal(null, grammar, "S")), ExpressionCommons.newPzero(null, ExpressionCommons.newNonTerminal(null, grammar, "ATTRIBUTE")),
				ExpressionCommons.newPzero(null, ExpressionCommons.newNonTerminal(null, grammar, "S")), ExpressionCommons.newString(null, "?>"), };
		grammar.newProduction(null, 0, "PROLOG", grammar.newSequence(l));
	}

	final void defDoctype() {
		Expression[] l = { ExpressionCommons.newString(null, "<!DOCTYPE"), ExpressionCommons.newPzero(null, ExpressionCommons.newNonTerminal(null, grammar, "S")), ExpressionCommons.newPzero(null, ExpressionCommons.newNonTerminal(null, grammar, "NAME")),
				ExpressionCommons.newPzero(null, ExpressionCommons.newNonTerminal(null, grammar, "S")), ExpressionCommons.newPoption(null, ExpressionCommons.newString(null, "SYSTEM")),
				ExpressionCommons.newPzero(null, ExpressionCommons.newNonTerminal(null, grammar, "S")), ExpressionCommons.newNonTerminal(null, grammar, "STRING"), ExpressionCommons.newPzero(null, ExpressionCommons.newNonTerminal(null, grammar, "S")),
				ExpressionCommons.newString(null, ">"), };
		grammar.newProduction(null, 0, "DOCTYPE", grammar.newSequence(l));
	}

	final void defContent() {
		Expression choice = grammar.newChoice(ExpressionCommons.newNonTerminal(null, grammar, "RootElement"), ExpressionCommons.newNonTerminal(null, grammar, "COMMENT"));
		Expression[] l = { ExpressionCommons.newPone(null, choice) };
		grammar.newProduction(null, 0, "Content", grammar.newSequence(l));
	}

	final void defName() {
		Expression[] l = { ExpressionCommons.newCharSet(null, "A-Za-z_:"), ExpressionCommons.newPzero(null, ExpressionCommons.newCharSet(null, "-A-Za-z0-9:._")) };
		grammar.newProduction(null, 0, "NAME", grammar.newSequence(l));
	}

	final void defString() {
		Expression[] l = { ExpressionCommons.newCbyte(null, false, '"'), grammar.newRepetition(ExpressionCommons.newPnot(null, ExpressionCommons.newCbyte(null, false, '"')), ExpressionCommons.newCany(null, false)),
				ExpressionCommons.newCbyte(null, false, '"') };
		grammar.newProduction(null, 0, "STRING", grammar.newSequence(l));
	}

	final void defNameToken() {
		Expression[] l = { ExpressionCommons.newCbyte(null, false, '"'), ExpressionCommons.newNonTerminal(null, grammar, "NAME"), ExpressionCommons.newCbyte(null, false, '"') };
		grammar.newProduction(null, 0, "NMTOKEN", grammar.newSequence(l));
	}

	final void defIdToken() {
		Expression[] l = { ExpressionCommons.newCharSet(null, "-A-Za-z0-9:._"), grammar.newRepetition(ExpressionCommons.newCharSet(null, "\\-A-Za-z0-9:._")), };
		grammar.newProduction(null, 0, "IDTOKEN", grammar.newSequence(l));
	}

	final void defText() {
		Expression onemoreExpr = grammar.newSequence(ExpressionCommons.newPnot(null, ExpressionCommons.newCharSet(null, "<&")), ExpressionCommons.newCany(null, false));
		grammar.newProduction(null, 0, "TEXT", ExpressionCommons.newPone(null, onemoreExpr));
	}

	final void defAttribute() {
		Expression[] l = { ExpressionCommons.newNonTerminal(null, grammar, "NAME"), grammar.newRepetition(ExpressionCommons.newNonTerminal(null, grammar, "S")), ExpressionCommons.newCbyte(null, false, '='),
				grammar.newRepetition(ExpressionCommons.newNonTerminal(null, grammar, "S")), ExpressionCommons.newNonTerminal(null, grammar, "STRING"), grammar.newRepetition(ExpressionCommons.newNonTerminal(null, grammar, "S")) };
		grammar.newProduction(null, 0, "ATTRIBUTE", grammar.newSequence(l));
	}

	final void defCDATASECT() {
		Expression[] l = { ExpressionCommons.newString(null, "<![CDATA["), grammar.newRepetition(ExpressionCommons.newPnot(null, ExpressionCommons.newString(null, "]]>")), ExpressionCommons.newCany(null, false)), ExpressionCommons.newString(null, "]]>") };
		grammar.newProduction(null, 0, "CDATASECT", grammar.newSequence(l));
	}

	final void defMISC() {
		grammar.newProduction(null, 0, "MISC", grammar.newChoice(ExpressionCommons.newNonTerminal(null, grammar, "S"), ExpressionCommons.newNonTerminal(null, grammar, "COMMENT")));
	}

	final void defCOMMENT() {
		Expression[] l = { ExpressionCommons.newString(null, "<!--"), grammar.newRepetition(ExpressionCommons.newPnot(null, ExpressionCommons.newString(null, "-->")), ExpressionCommons.newCany(null, false)), ExpressionCommons.newString(null, "-->"),
				grammar.newRepetition(ExpressionCommons.newNonTerminal(null, grammar, "S")) };
		grammar.newProduction(null, 0, "COMMENT", grammar.newSequence(l));
	}

	final void defEMPTY() {
		grammar.newProduction(null, 0, "EMPTY", grammar.newRepetition(ExpressionCommons.newNonTerminal(null, grammar, "S")));
	}

	final void defANY() {
		Expression[] l = { grammar.newRepetition(ExpressionCommons.newNonTerminal(null, grammar, "S")), ExpressionCommons.newNonTerminal(null, grammar, "TEXT"), grammar.newRepetition(ExpressionCommons.newNonTerminal(null, grammar, "S")), };
		grammar.newProduction(null, 0, "ANY", grammar.newSequence(l));
	}

	final void defSpacing() {
		grammar.newProduction(null, 0, "S", ExpressionCommons.newCharSet(null, " \t\r\n"));
	}

	final void defENDTAG() {
		Expression l = grammar.newChoice(ExpressionCommons.newPand(null, ExpressionCommons.newCbyte(null, false, '>')), ExpressionCommons.newPand(null, ExpressionCommons.newString(null, "/>")));
		grammar.newProduction(null, 0, "ENDTAG", l);
	}

	final void defNotAny() {
		Expression l = ExpressionCommons.newPnot(null, ExpressionCommons.newCany(null, false));
		grammar.newProduction(null, 0, "NotAny", l);
	}

	final void defPCDATA() {
		Expression[] seq = { ExpressionCommons.newNonTerminal(null, grammar, "PreEntity"), ExpressionCommons.newNonTerminal(null, grammar, "Entity") };
		Expression[] l = { ExpressionCommons.newNonTerminal(null, grammar, "TEXT"), grammar.newSequence(ExpressionCommons.newCbyte(null, false, '&'), grammar.newChoice(seq), ExpressionCommons.newCbyte(null, false, ';')) };
		grammar.newProduction(null, 0, "PCDATA", grammar.newChoice(l));
	}

	final void defRootElement() {
		String rootElementName = "Element_" + this.rootElement;
		grammar.newProduction(null, 0, "RootElement", ExpressionCommons.newNonTerminal(null, grammar, rootElementName));
	}

	final void defPreEntity() {
		Expression[] keywords = { ExpressionCommons.newString(null, "lt"), ExpressionCommons.newString(null, "gt"), ExpressionCommons.newString(null, "amp"), ExpressionCommons.newString(null, "apos"), ExpressionCommons.newString(null, "quot"),
				grammar.newRepetition1(ExpressionCommons.newCharSet(null, "#a-zA-Z0-9")) };
		grammar.newProduction(null, 0, "PreEntity", grammar.newChoice(keywords));
	}
}

class Permutation {
	private int number, list_size;
	private int[] perm, target;
	private boolean[] flag;
	private int[][] perm_list;
	private int perm_list_index;

	public Permutation(int[] target) {
		this.target = target;
		this.number = target.length;
		this.list_size = this.fact(this.number);
		this.perm = new int[this.number];
		this.flag = new boolean[this.number + 1];
		this.perm_list = new int[this.list_size][this.number];
		this.perm_list_index = 0;
		this.createPermutation(0, this.target);
	}

	public int[][] getPermList() {
		return this.perm_list;
	}

	private int fact(int n) {
		return n == 0 ? 1 : n * fact(n - 1);
	}

	public void createPermutation(int n, int[] target) {
		if (n == this.number) {
			for (int i = 0; i < n; i++) {
				perm_list[perm_list_index][i] = perm[i];
			}
			perm_list_index++;
		} else {
			for (int i = 0; i < perm.length; i++) {
				if (flag[i])
					continue;
				perm[n] = target[i];
				flag[i] = true;
				createPermutation(n + 1, target);
				flag[i] = false;
			}
		}
	}
}
