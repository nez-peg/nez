package nez.ext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nez.Grammar;
import nez.Parser;
import nez.Strategy;
import nez.ast.AbstractTree;
import nez.ast.Symbol;
import nez.lang.Expression;
import nez.lang.GrammarFile;
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
	public Parser getLoaderGrammar() {
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

	private final void loadPredefinedRules(AbstractTree<?> node) {
		String rootElement = node.get(0).getText(0, null);
		PredefinedRules preRules = new PredefinedRules(getGrammarFile(), rootElement);
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
	public void parse(AbstractTree<?> node) {
		loadPredefinedRules(node);
		visit("visit", node);
	}

	public void visitDoc(AbstractTree<?> node) {
		GrammarFile gfile = getGrammarFile();
		for (AbstractTree<?> subnode : node) {
			this.visit("visit", subnode);
		}
		for (int elementID = 0; elementID < elementCount; elementID++) {
			String elementName = "Element_" + elementNameMap.get(elementID);
			gfile.addProduction(node, elementName, genElement(node, elementID));
		}
		gfile.addProduction(node, "Entity", genEntityList(node));
	}

	public void visitElement(AbstractTree<?> node) {
		String elementName = node.getText(0, "");
		elementNameMap.put(elementCount, elementName);
		containsAttributeList.add(false);
		getGrammarFile().addProduction(node, "Content" + elementCount, toExpression(node.get(1)));
		elementCount++;
	}

	private Expression genElement(AbstractTree<?> node, int elementID) {
		GrammarFile gfile = getGrammarFile();
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
		GrammarFile gfile = getGrammarFile();
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

	public void visitREQUIRED(AbstractTree<?> node) {
		String name = "AttDef" + currentElementID + "_" + attDefCount;
		attDefMap.put(attDefCount, node.getText(0, ""));
		requiredAttList.add(attDefCount);
		getGrammarFile().addProduction(node, name, toExpression(node.get(1)));
		attDefCount++;
	}

	public void visitIMPLIED(AbstractTree<?> node) {
		String name = "AttDef" + currentElementID + "_" + attDefCount;
		attDefMap.put(attDefCount, node.getText(0, ""));
		impliedAttList.add(attDefCount);
		getGrammarFile().addProduction(node, name, toExpression(node.get(1)));
		attDefCount++;
	}

	public void visitFIXED(AbstractTree<?> node) {
		String name = "AttDef" + currentElementID + "_" + attDefCount;
		attDefMap.put(attDefCount, node.getText(0, ""));
		impliedAttList.add(attDefCount);
		getGrammarFile().addProduction(node, name, genFixedAtt(node));
		attDefCount++;
	}

	public void visitDefault(AbstractTree<?> node) {
		String name = "AttDef" + currentElementID + "_" + attDefCount;
		attDefMap.put(attDefCount, node.getText(0, ""));
		impliedAttList.add(attDefCount);
		getGrammarFile().addProduction(node, name, toExpression(node.get(1)));
		attDefCount++;
	}

	public void visitEntity(AbstractTree<?> node) {
		String name = "ENT_" + entityCount++;
		getGrammarFile().addProduction(node, name, toExpression(node.get(1)));
	}

	private Expression toExpression(AbstractTree<?> node) {
		return (Expression) this.visit("to", node);
	}

	public Expression toEmpty(AbstractTree<?> node) {
		return ExpressionCommons.newNonTerminal(null, getGrammarFile(), "EMPTY");
	}

	public Expression toAny(AbstractTree<?> node) {
		return ExpressionCommons.newNonTerminal(null, getGrammarFile(), "ANY");
	}

	public Expression toZeroMore(AbstractTree<?> node) {
		return getGrammarFile().newRepetition(toExpression(node.get(0)));
	}

	public Expression toOneMore(AbstractTree<?> node) {
		return getGrammarFile().newRepetition1(toExpression(node.get(0)));
	}

	public Expression toOption(AbstractTree<?> node) {
		return getGrammarFile().newOption(toExpression(node.get(0)));
	}

	public Expression toChoice(AbstractTree<?> node) {
		Expression[] l = new Expression[node.size()];
		int count = 0;
		for (AbstractTree<?> subnode : node) {
			l[count++] = toExpression(subnode);
		}
		return getGrammarFile().newChoice(l);
	}

	public Expression toSeq(AbstractTree<?> node) {
		Expression[] l = new Expression[node.size()];
		int count = 0;
		for (AbstractTree<?> subnode : node) {
			l[count++] = toExpression(subnode);
		}
		return getGrammarFile().newSequence(l);
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
		GrammarFile gfile = getGrammarFile();
		String attName = attDefMap.get(attDefCount);
		Expression[] l = { gfile.newString(attName), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), gfile.newByteChar('='), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")),
				ExpressionCommons.newNonTerminal(null, gfile, type), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), };
		return gfile.newSequence(l);
	}

	public Expression _AttQ(String type) {
		GrammarFile gfile = getGrammarFile();
		String attName = attDefMap.get(attDefCount);
		Expression[] l = { gfile.newString(attName), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), gfile.newByteChar('='), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), gfile.newString("\""),
				ExpressionCommons.newNonTerminal(null, gfile, type), gfile.newString("\""), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), };
		return gfile.newSequence(l);
	}

	public Expression _AttDef(String type) {
		GrammarFile gfile = getGrammarFile();
		String attName = attDefMap.get(attDefCount);
		Expression[] l = { ExpressionCommons.newXdef(null, gfile, Symbol.tag("T" + currentElementID), gfile.newString(attName)), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), gfile.newByteChar('='),
				gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), ExpressionCommons.newNonTerminal(null, gfile, type), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), };
		return gfile.newSequence(l);
	}

	public Expression _AttDefQ(String type) {
		GrammarFile gfile = getGrammarFile();
		String attName = attDefMap.get(attDefCount);
		Expression[] l = { ExpressionCommons.newXdef(null, gfile, Symbol.tag("T" + currentElementID), gfile.newString(attName)), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), gfile.newByteChar('='),
				gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), gfile.newString("\""), ExpressionCommons.newNonTerminal(null, gfile, type), gfile.newString("\""),
				gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), };
		return gfile.newSequence(l);
	}

	private Expression genFixedAtt(AbstractTree<?> node) {
		GrammarFile gfile = getGrammarFile();
		String attName = attDefMap.get(attDefCount);
		String fixedValue = "\"" + node.getText(2, "") + "\"";
		Expression[] l = { gfile.newString(attName), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), gfile.newByteChar('='), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), gfile.newString(fixedValue),
				gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")),

		};
		return gfile.newSequence(l);
	}

	public Expression genExAtt(AbstractTree<?> node, int[] requiredList) {
		GrammarFile gfile = getGrammarFile();
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

	public Expression genExAttDefList(AbstractTree<?> node, int[] requiredList, Symbol tableName) {
		GrammarFile gfile = getGrammarFile();
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

	public Expression genCompAtt(AbstractTree<?> node, int[] attlist) {
		GrammarFile gfile = getGrammarFile();
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
		GrammarFile gfile = getGrammarFile();
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

	public Expression genImpliedChoice(AbstractTree<?> node) {
		GrammarFile gfile = getGrammarFile();
		Expression[] l = new Expression[impliedAttList.size()];
		String definitionName = "AttDef" + currentElementID + "_";
		int choiceCount = 0;
		for (Integer ruleNum : impliedAttList) {
			l[choiceCount++] = ExpressionCommons.newNonTerminal(null, gfile, definitionName + ruleNum);
		}
		return gfile.newChoice(l);
	}

	public Expression toEnum(AbstractTree<?> node) {
		GrammarFile gfile = getGrammarFile();
		String attName = attDefMap.get(attDefCount);
		Expression[] l = { gfile.newString(attName), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), gfile.newByteChar('='), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), gfile.newByteChar('"'),
				toChoice(node), gfile.newByteChar('"'), gfile.newRepetition(ExpressionCommons.newNonTerminal(null, gfile, "S")), };
		return gfile.newSequence(l);
	}

	public Expression toEntValue(AbstractTree<?> node) {
		String replaceString = node.toText();
		return getGrammarFile().newString(replaceString);
	}

	public Expression toElName(AbstractTree<?> node) {
		String elementName = "Element_" + node.toText();
		return ExpressionCommons.newNonTerminal(null, getGrammarFile(), elementName);
	}

	public Expression toName(AbstractTree<?> node) {
		return getGrammarFile().newString(node.toText());
	}

	public Expression toData(AbstractTree<?> node) {
		return ExpressionCommons.newNonTerminal(null, getGrammarFile(), "PCDATA");
	}

	public Expression toOnlyData(AbstractTree<?> node) {
		return ExpressionCommons.newPzero(null, ExpressionCommons.newNonTerminal(null, getGrammarFile(), "PCDATA"));
	}

	private Expression genEntityList(AbstractTree<?> node) {
		GrammarFile gfile = getGrammarFile();
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
	GrammarFile grammar;
	String rootElement;

	public PredefinedRules(GrammarFile grammar, String rootElement) {
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
		grammar.addProduction(null, "Toplevel", ExpressionCommons.newNonTerminal(null, grammar, "Document"));

	}

	final void defChunk() {
		grammar.addProduction(null, "Chunk", ExpressionCommons.newNonTerminal(null, grammar, "Document"));
	}

	final void defFile() {
		grammar.addProduction(null, "File", ExpressionCommons.newNonTerminal(null, grammar, "Document"));
	}

	final void defDocument() {
		Expression[] l = { ExpressionCommons.newPoption(null, ExpressionCommons.newNonTerminal(null, grammar, "PROLOG")), ExpressionCommons.newPzero(null, ExpressionCommons.newNonTerminal(null, grammar, "MISC")),
				ExpressionCommons.newPoption(null, ExpressionCommons.newNonTerminal(null, grammar, "DOCTYPE")), ExpressionCommons.newPzero(null, ExpressionCommons.newNonTerminal(null, grammar, "MISC")),
				ExpressionCommons.newNonTerminal(null, grammar, "Content"), ExpressionCommons.newPzero(null, ExpressionCommons.newNonTerminal(null, grammar, "MISC")), };
		grammar.addProduction(null, "Document", grammar.newSequence(l));
	}

	final void defProlog() {
		Expression[] l = { ExpressionCommons.newString(null, "<?xml"), ExpressionCommons.newPzero(null, ExpressionCommons.newNonTerminal(null, grammar, "S")), ExpressionCommons.newPzero(null, ExpressionCommons.newNonTerminal(null, grammar, "ATTRIBUTE")),
				ExpressionCommons.newPzero(null, ExpressionCommons.newNonTerminal(null, grammar, "S")), ExpressionCommons.newString(null, "?>"), };
		grammar.addProduction(null, "PROLOG", grammar.newSequence(l));
	}

	final void defDoctype() {
		Expression[] l = { ExpressionCommons.newString(null, "<!DOCTYPE"), ExpressionCommons.newPzero(null, ExpressionCommons.newNonTerminal(null, grammar, "S")), ExpressionCommons.newPzero(null, ExpressionCommons.newNonTerminal(null, grammar, "NAME")),
				ExpressionCommons.newPzero(null, ExpressionCommons.newNonTerminal(null, grammar, "S")), ExpressionCommons.newPoption(null, ExpressionCommons.newString(null, "SYSTEM")),
				ExpressionCommons.newPzero(null, ExpressionCommons.newNonTerminal(null, grammar, "S")), ExpressionCommons.newNonTerminal(null, grammar, "STRING"), ExpressionCommons.newPzero(null, ExpressionCommons.newNonTerminal(null, grammar, "S")),
				ExpressionCommons.newString(null, ">"), };
		grammar.addProduction(null, "DOCTYPE", grammar.newSequence(l));
	}

	final void defContent() {
		Expression choice = grammar.newChoice(ExpressionCommons.newNonTerminal(null, grammar, "RootElement"), ExpressionCommons.newNonTerminal(null, grammar, "COMMENT"));
		Expression[] l = { ExpressionCommons.newPone(null, choice) };
		grammar.addProduction(null, "Content", grammar.newSequence(l));
	}

	final void defName() {
		Expression[] l = { ExpressionCommons.newCharSet(null, "A-Za-z_:"), ExpressionCommons.newPzero(null, ExpressionCommons.newCharSet(null, "-A-Za-z0-9:._")) };
		grammar.addProduction(null, "NAME", grammar.newSequence(l));
	}

	final void defString() {
		Expression[] l = { ExpressionCommons.newCbyte(null, false, '"'), grammar.newRepetition(ExpressionCommons.newPnot(null, ExpressionCommons.newCbyte(null, false, '"')), ExpressionCommons.newCany(null, false)),
				ExpressionCommons.newCbyte(null, false, '"') };
		grammar.addProduction(null, "STRING", grammar.newSequence(l));
	}

	final void defNameToken() {
		Expression[] l = { ExpressionCommons.newCbyte(null, false, '"'), ExpressionCommons.newNonTerminal(null, grammar, "NAME"), ExpressionCommons.newCbyte(null, false, '"') };
		grammar.addProduction(null, "NMTOKEN", grammar.newSequence(l));
	}

	final void defIdToken() {
		Expression[] l = { ExpressionCommons.newCharSet(null, "-A-Za-z0-9:._"), grammar.newRepetition(ExpressionCommons.newCharSet(null, "\\-A-Za-z0-9:._")), };
		grammar.addProduction(null, "IDTOKEN", grammar.newSequence(l));
	}

	final void defText() {
		Expression onemoreExpr = grammar.newSequence(ExpressionCommons.newPnot(null, ExpressionCommons.newCharSet(null, "<&")), ExpressionCommons.newCany(null, false));
		grammar.addProduction(null, "TEXT", ExpressionCommons.newPone(null, onemoreExpr));
	}

	final void defAttribute() {
		Expression[] l = { ExpressionCommons.newNonTerminal(null, grammar, "NAME"), grammar.newRepetition(ExpressionCommons.newNonTerminal(null, grammar, "S")), ExpressionCommons.newCbyte(null, false, '='),
				grammar.newRepetition(ExpressionCommons.newNonTerminal(null, grammar, "S")), ExpressionCommons.newNonTerminal(null, grammar, "STRING"), grammar.newRepetition(ExpressionCommons.newNonTerminal(null, grammar, "S")) };
		grammar.addProduction(null, "ATTRIBUTE", grammar.newSequence(l));
	}

	final void defCDATASECT() {
		Expression[] l = { ExpressionCommons.newString(null, "<![CDATA["), grammar.newRepetition(ExpressionCommons.newPnot(null, ExpressionCommons.newString(null, "]]>")), ExpressionCommons.newCany(null, false)), ExpressionCommons.newString(null, "]]>") };
		grammar.addProduction(null, "CDATASECT", grammar.newSequence(l));
	}

	final void defMISC() {
		grammar.addProduction(null, "MISC", grammar.newChoice(ExpressionCommons.newNonTerminal(null, grammar, "S"), ExpressionCommons.newNonTerminal(null, grammar, "COMMENT")));
	}

	final void defCOMMENT() {
		Expression[] l = { ExpressionCommons.newString(null, "<!--"), grammar.newRepetition(ExpressionCommons.newPnot(null, ExpressionCommons.newString(null, "-->")), ExpressionCommons.newCany(null, false)), ExpressionCommons.newString(null, "-->"),
				grammar.newRepetition(ExpressionCommons.newNonTerminal(null, grammar, "S")) };
		grammar.addProduction(null, "COMMENT", grammar.newSequence(l));
	}

	final void defEMPTY() {
		grammar.addProduction(null, "EMPTY", grammar.newRepetition(ExpressionCommons.newNonTerminal(null, grammar, "S")));
	}

	final void defANY() {
		Expression[] l = { grammar.newRepetition(ExpressionCommons.newNonTerminal(null, grammar, "S")), ExpressionCommons.newNonTerminal(null, grammar, "TEXT"), grammar.newRepetition(ExpressionCommons.newNonTerminal(null, grammar, "S")), };
		grammar.addProduction(null, "ANY", grammar.newSequence(l));
	}

	final void defSpacing() {
		grammar.addProduction(null, "S", ExpressionCommons.newCharSet(null, " \t\r\n"));
	}

	final void defENDTAG() {
		Expression l = grammar.newChoice(ExpressionCommons.newPand(null, ExpressionCommons.newCbyte(null, false, '>')), ExpressionCommons.newPand(null, ExpressionCommons.newString(null, "/>")));
		grammar.addProduction(null, "ENDTAG", l);
	}

	final void defNotAny() {
		Expression l = ExpressionCommons.newPnot(null, ExpressionCommons.newCany(null, false));
		grammar.addProduction(null, "NotAny", l);
	}

	final void defPCDATA() {
		Expression[] seq = { ExpressionCommons.newNonTerminal(null, grammar, "PreEntity"), ExpressionCommons.newNonTerminal(null, grammar, "Entity") };
		Expression[] l = { ExpressionCommons.newNonTerminal(null, grammar, "TEXT"), grammar.newSequence(ExpressionCommons.newCbyte(null, false, '&'), grammar.newChoice(seq), ExpressionCommons.newCbyte(null, false, ';')) };
		grammar.addProduction(null, "PCDATA", grammar.newChoice(l));
	}

	final void defRootElement() {
		String rootElementName = "Element_" + this.rootElement;
		grammar.addProduction(null, "RootElement", ExpressionCommons.newNonTerminal(null, grammar, rootElementName));
	}

	final void defPreEntity() {
		Expression[] keywords = { ExpressionCommons.newString(null, "lt"), ExpressionCommons.newString(null, "gt"), ExpressionCommons.newString(null, "amp"), ExpressionCommons.newString(null, "apos"), ExpressionCommons.newString(null, "quot"),
				grammar.newRepetition1(ExpressionCommons.newCharSet(null, "#a-zA-Z0-9")) };
		grammar.addProduction(null, "PreEntity", grammar.newChoice(keywords));
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
