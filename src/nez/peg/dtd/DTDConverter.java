package nez.peg.dtd;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nez.NezException;
import nez.NezOption;
import nez.SourceContext;
import nez.ast.AbstractTreeVisitor;
import nez.ast.CommonTree;
import nez.lang.Expression;
import nez.lang.Grammar;
import nez.lang.GrammarFactory;
import nez.lang.GrammarFile;
import nez.util.ConsoleUtils;

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
		Grammar p = dtdGrammar.newGrammar("File");
		SourceContext dtdFile = SourceContext.newFileContext(filePath);
		CommonTree node = p.parseCommonTree(dtdFile);
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

	final void convert(CommonTree node, GrammarFile grammar) {
		this.gfile = grammar;
		this.loadPredfinedRules(node);
		this.visit("visit", node);
	}

	final void loadPredfinedRules(CommonTree node) {
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

	public void visitDoc(CommonTree node) {
		for (CommonTree subnode : node) {
			this.visit("visit", subnode);
		}
		for (int elementID = 0; elementID < elementCount; elementID++) {
			String elementName = "Element_" + elementNameMap.get(elementID);
			gfile.defineProduction(node, elementName, genElement(node, elementID));
		}
		gfile.defineProduction(node, "Entity", genEntityList(node));
	}

	public void visitElement(CommonTree node) {
		String elementName = node.getText(0, "");
		elementNameMap.put(elementCount, elementName);
		containsAttributeList.add(false);
		gfile.defineProduction(node, "Content" + elementCount, toExpression(node.get(1)));
		elementCount++;
	}

	private Expression genElement(CommonTree node, int elementID) {
		String elementName = elementNameMap.get(elementID);
		Expression[] contentSeq = { gfile.newByteChar('>'), gfile.newRepetition(GrammarFactory.newNonTerminal(null, gfile, "S")), GrammarFactory.newNonTerminal(null, gfile, "Content" + elementID),
				gfile.newRepetition(GrammarFactory.newNonTerminal(null, gfile, "S")), gfile.newString("</" + elementName + ">"), };
		Expression[] attOnlySeq = { gfile.newString("/>"), };
		Expression[] endChoice = { gfile.newSequence(attOnlySeq), gfile.newSequence(contentSeq) };
		// check whether attribute exists
		if (hasAttribute(elementID)) {
			Expression[] l = { gfile.newString("<" + elementName), gfile.newRepetition(GrammarFactory.newNonTerminal(null, gfile, "S")), GrammarFactory.newNonTerminal(null, gfile, "Attribute" + elementID),
					gfile.newRepetition(GrammarFactory.newNonTerminal(null, gfile, "S")), gfile.newChoice(endChoice), gfile.newRepetition(GrammarFactory.newNonTerminal(null, gfile, "S")), };
			return gfile.newSequence(l);
		} else {
			Expression[] l = { gfile.newString("<" + elementName), gfile.newRepetition(GrammarFactory.newNonTerminal(null, gfile, "S")), gfile.newChoice(endChoice), gfile.newRepetition(GrammarFactory.newNonTerminal(null, gfile, "S")),

			};
			return gfile.newSequence(l);
		}
	}

	public void visitAttlist(CommonTree node) {
		initAttCounter();
		containsAttributeList.add(currentElementID, true);
		String attListName = "Attribute" + currentElementID;
		String choiceListName = "AttChoice" + currentElementID;
		for (int attNum = 1; attNum < node.size(); attNum++) {
			this.visit("visit", node.get(attNum));
		}
		int[] attDefList = initAttDefList();
		// generate Complete / Proximate Attribute list
		if (impliedAttList.isEmpty()) {
			gfile.defineProduction(node, attListName, genCompAtt(node, attDefList));
		} else {
			int[] requiredRules = extractRequiredRule(attDefList);
			gfile.defineProduction(node, choiceListName, genImpliedChoice(node));
			gfile.defineProduction(node, attListName, genProxAtt(node, requiredRules));
		}
	}

	public void visitREQUIRED(CommonTree node) {
		String name = "AttDef" + currentElementID + "_" + attDefCount;
		attDefMap.put(attDefCount, node.getText(0, ""));
		requiredAttList.add(attDefCount);
		gfile.defineProduction(node, name, toExpression(node.get(1)));
		attDefCount++;
	}

	public void visitIMPLIED(CommonTree node) {
		String name = "AttDef" + currentElementID + "_" + attDefCount;
		attDefMap.put(attDefCount, node.getText(0, ""));
		impliedAttList.add(attDefCount);
		gfile.defineProduction(node, name, toExpression(node.get(1)));
		attDefCount++;
	}

	public void visitFIXED(CommonTree node) {
		String name = "AttDef" + currentElementID + "_" + attDefCount;
		attDefMap.put(attDefCount, node.getText(0, ""));
		impliedAttList.add(attDefCount);
		gfile.defineProduction(node, name, genFixedAtt(node));
		attDefCount++;
	}

	public void visitDefault(CommonTree node) {
		String name = "AttDef" + currentElementID + "_" + attDefCount;
		attDefMap.put(attDefCount, node.getText(0, ""));
		impliedAttList.add(attDefCount);
		gfile.defineProduction(node, name, toExpression(node.get(1)));
		attDefCount++;
	}

	public void visitEntity(CommonTree node) {
		String name = "ENT_" + entityCount++;
		gfile.defineProduction(node, name, toExpression(node.get(1)));
	}

	private Expression toExpression(CommonTree node) {
		return (Expression) this.visit("to", node);
	}

	public Expression toEmpty(CommonTree node) {
		return GrammarFactory.newNonTerminal(null, gfile, "EMPTY");
	}

	public Expression toAny(CommonTree node) {
		return GrammarFactory.newNonTerminal(null, gfile, "ANY");
	}

	public Expression toZeroMore(CommonTree node) {
		return gfile.newRepetition(toExpression(node.get(0)));
	}

	public Expression toOneMore(CommonTree node) {
		return gfile.newRepetition1(toExpression(node.get(0)));
	}

	public Expression toOption(CommonTree node) {
		return gfile.newOption(toExpression(node.get(0)));
	}

	public Expression toChoice(CommonTree node) {
		Expression[] l = new Expression[node.size()];
		int count = 0;
		for (CommonTree subnode : node) {
			l[count++] = toExpression(subnode);
		}
		return gfile.newChoice(l);
	}

	public Expression toSeq(CommonTree node) {
		Expression[] l = new Expression[node.size()];
		int count = 0;
		for (CommonTree subnode : node) {
			l[count++] = toExpression(subnode);
		}
		return gfile.newSequence(l);
	}

	public Expression toCDATA(CommonTree node) {
		String attName = attDefMap.get(attDefCount);
		Expression[] l = { gfile.newString(attName), gfile.newRepetition(GrammarFactory.newNonTerminal(null, gfile, "S")), gfile.newByteChar('='), gfile.newRepetition(GrammarFactory.newNonTerminal(null, gfile, "S")),
				GrammarFactory.newNonTerminal(null, gfile, "STRING"), gfile.newRepetition(GrammarFactory.newNonTerminal(null, gfile, "S")), };
		return gfile.newSequence(l);
	}

	public Expression toID(CommonTree node) {
		String attName = attDefMap.get(attDefCount);
		Expression[] l = { gfile.newString(attName), gfile.newRepetition(GrammarFactory.newNonTerminal(null, gfile, "S")), gfile.newByteChar('='), gfile.newRepetition(GrammarFactory.newNonTerminal(null, gfile, "S")), gfile.newString("\""),
				GrammarFactory.newNonTerminal(null, gfile, "IDTOKEN"), gfile.newString("\""), gfile.newRepetition(GrammarFactory.newNonTerminal(null, gfile, "S")),
		// grammar.newDefSymbol(Tag.tag("IDLIST"),
		// grammar.newNonTerminal("IDTOKEN")));
		};
		return gfile.newSequence(l);

	}

	public Expression toIDREF(CommonTree node) {
		String attName = attDefMap.get(attDefCount);
		Expression[] l = { gfile.newString(attName), gfile.newRepetition(GrammarFactory.newNonTerminal(null, gfile, "S")), gfile.newByteChar('='), gfile.newRepetition(GrammarFactory.newNonTerminal(null, gfile, "S")), gfile.newString("\""),
				GrammarFactory.newNonTerminal(null, gfile, "IDTOKEN"), gfile.newString("\""), gfile.newRepetition(GrammarFactory.newNonTerminal(null, gfile, "S")), };
		return gfile.newSequence(l);
	}

	public Expression toIDREFS(CommonTree node) {
		String attName = attDefMap.get(attDefCount);
		Expression[] l = { gfile.newString(attName), gfile.newRepetition(GrammarFactory.newNonTerminal(null, gfile, "S")), gfile.newByteChar('='), gfile.newRepetition(GrammarFactory.newNonTerminal(null, gfile, "S")), gfile.newString("\""),
				GrammarFactory.newNonTerminal(null, gfile, "IDTOKENS"), gfile.newString("\""), gfile.newRepetition(GrammarFactory.newNonTerminal(null, gfile, "S")),
		// (grammar.newRepetition(node, grammar.newIsaSymbol(node,
		// Tag.tag("IDLIST"))));
		};
		return gfile.newSequence(l);
	}

	private Expression genFixedAtt(CommonTree node) {
		String attName = attDefMap.get(attDefCount);
		String fixedValue = "\"" + node.getText(2, "") + "\"";
		Expression[] l = { gfile.newString(attName), gfile.newRepetition(GrammarFactory.newNonTerminal(null, gfile, "S")), gfile.newByteChar('='), gfile.newRepetition(GrammarFactory.newNonTerminal(null, gfile, "S")), gfile.newString(fixedValue),
				gfile.newRepetition(GrammarFactory.newNonTerminal(null, gfile, "S")),

		};
		return gfile.newSequence(l);
	}

	public Expression toENTITY(CommonTree node) {
		String attName = attDefMap.get(attDefCount);
		Expression[] l = { gfile.newString(attName), gfile.newRepetition(GrammarFactory.newNonTerminal(null, gfile, "S")), gfile.newByteChar('='), gfile.newRepetition(GrammarFactory.newNonTerminal(null, gfile, "S")), gfile.newString("\""),
				GrammarFactory.newNonTerminal(null, gfile, "entity"), gfile.newString("\""), gfile.newRepetition(GrammarFactory.newNonTerminal(null, gfile, "S")), };
		return gfile.newSequence(l);
	}

	public Expression toENTITIES(CommonTree node) {
		String attName = attDefMap.get(attDefCount);
		Expression[] l = { gfile.newString(attName), gfile.newRepetition(GrammarFactory.newNonTerminal(null, gfile, "S")), gfile.newByteChar('='), gfile.newRepetition(GrammarFactory.newNonTerminal(null, gfile, "S")), gfile.newString("\""),
				GrammarFactory.newNonTerminal(null, gfile, "entities"), gfile.newString("\""), gfile.newRepetition(GrammarFactory.newNonTerminal(null, gfile, "S")), };
		return gfile.newSequence(l);
	}

	public Expression toNMTOKEN(CommonTree node) {
		String attName = attDefMap.get(attDefCount);
		Expression[] l = { gfile.newString(attName), gfile.newRepetition(GrammarFactory.newNonTerminal(null, gfile, "S")), gfile.newByteChar('='), gfile.newRepetition(GrammarFactory.newNonTerminal(null, gfile, "S")),
				GrammarFactory.newNonTerminal(null, gfile, "NMTOKEN"), gfile.newRepetition(GrammarFactory.newNonTerminal(null, gfile, "S")), };
		return gfile.newSequence(l);
	}

	public Expression toNMTOKENS(CommonTree node) {
		String attName = attDefMap.get(attDefCount);
		Expression[] l = { gfile.newString(attName), gfile.newRepetition(GrammarFactory.newNonTerminal(null, gfile, "S")), gfile.newByteChar('='), gfile.newRepetition(GrammarFactory.newNonTerminal(null, gfile, "S")),
				GrammarFactory.newNonTerminal(null, gfile, "NMTOKEN"), gfile.newRepetition(GrammarFactory.newNonTerminal(null, gfile, "S")), };
		return gfile.newSequence(l);
	}

	public Expression genCompAtt(CommonTree node, int[] attlist) {
		int listLength = attlist.length;
		if (listLength == 1) {
			Expression[] l = { GrammarFactory.newNonTerminal(null, gfile, "AttDef" + currentElementID + "_" + attlist[0]), gfile.newRepetition(GrammarFactory.newNonTerminal(null, gfile, "S")), GrammarFactory.newNonTerminal(null, gfile, "ENDTAG") };
			return gfile.newSequence(l);
		} else {
			int[][] permutationList = perm(attlist);
			Expression[] choiceList = new Expression[permutationList.length];
			int choiceCount = 0;
			for (int[] target : permutationList) {
				Expression[] seqList = new Expression[listLength + 1];
				for (int index = 0; index < target.length; index++) {
					seqList[index] = GrammarFactory.newNonTerminal(null, gfile, "AttDef" + currentElementID + "_" + target[index]);
				}
				seqList[listLength] = GrammarFactory.newNonTerminal(null, gfile, "ENDTAG");
				choiceList[choiceCount++] = gfile.newSequence(seqList);
			}
			return gfile.newChoice(choiceList);
		}
	}

	public Expression genProxAtt(CommonTree node, int[] attlist) {
		int listLength = attlist.length;
		if (listLength == 0) {
			Expression[] l = { gfile.newRepetition(GrammarFactory.newNonTerminal(null, gfile, "AttChoice" + currentElementID)), gfile.newRepetition(GrammarFactory.newNonTerminal(null, gfile, "S")), GrammarFactory.newNonTerminal(null, gfile, "ENDTAG") };
			return gfile.newSequence(l);
		} else {
			int[][] permedList = perm(attlist);
			Expression[] choiceList = new Expression[permedList.length];
			int choiceCount = 0;
			for (int[] target : permedList) {
				Expression[] seqList = new Expression[listLength * 2 + 2];
				int seqCount = 0;
				seqList[seqCount++] = gfile.newRepetition(GrammarFactory.newNonTerminal(null, gfile, "AttChoice" + currentElementID));
				for (int index = 0; index < target.length; index++) {
					seqList[seqCount++] = GrammarFactory.newNonTerminal(null, gfile, "AttDef" + currentElementID + "_" + target[index]);
					seqList[seqCount++] = gfile.newRepetition(GrammarFactory.newNonTerminal(null, gfile, "AttChoice" + currentElementID));
				}
				seqList[seqCount] = GrammarFactory.newNonTerminal(null, gfile, "ENDTAG");
				choiceList[choiceCount++] = gfile.newSequence(seqList);
			}
			return gfile.newChoice(choiceList);
		}
	}

	public Expression genImpliedChoice(CommonTree node) {
		Expression[] l = new Expression[impliedAttList.size()];
		String definitionName = "AttDef" + currentElementID + "_";
		int choiceCount = 0;
		for (Integer ruleNum : impliedAttList) {
			l[choiceCount++] = GrammarFactory.newNonTerminal(null, gfile, definitionName + ruleNum);
		}
		return gfile.newChoice(l);
	}

	public Expression toEnum(CommonTree node) {
		String attName = attDefMap.get(attDefCount);
		Expression[] l = { gfile.newString(attName), gfile.newRepetition(GrammarFactory.newNonTerminal(null, gfile, "S")), gfile.newByteChar('='), gfile.newRepetition(GrammarFactory.newNonTerminal(null, gfile, "S")), gfile.newByteChar('"'),
				toChoice(node), gfile.newByteChar('"'), gfile.newRepetition(GrammarFactory.newNonTerminal(null, gfile, "S")), };
		return gfile.newSequence(l);
	}

	public Expression toEntValue(CommonTree node) {
		String replaceString = node.toText();
		return gfile.newString(replaceString);
	}

	public Expression toElName(CommonTree node) {
		String elementName = "Element_" + node.toText();
		return GrammarFactory.newNonTerminal(null, gfile, elementName);
	}

	public Expression toName(CommonTree node) {
		return gfile.newString(node.toText());
	}

	public Expression toData(CommonTree node) {
		return GrammarFactory.newNonTerminal(null, gfile, "PCDATA");
	}

	public Expression toOnlyData(CommonTree node) {
		return gfile.newRepetition(GrammarFactory.newNonTerminal(null, gfile, "PCDATA"));
	}

	private Expression genEntityList(CommonTree node) {
		if (entityCount == 0) {
			return GrammarFactory.newFailure(null);
		} else {
			Expression[] l = new Expression[entityCount];
			for (int entityNum = 0; entityNum < entityCount; entityNum++) {
				l[entityNum] = GrammarFactory.newNonTerminal(null, gfile, "ENT_" + entityNum);
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
