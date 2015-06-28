package nez.peg.dtd;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nez.NezOption;
import nez.NezException;
import nez.SourceContext;
import nez.ast.CommonTree;
import nez.ast.CommonTreeVisitor;
import nez.lang.Expression;
import nez.lang.Grammar;
import nez.lang.GrammarChecker;
import nez.lang.GrammarFactory;
import nez.lang.GrammarFile;
import nez.util.ConsoleUtils;

public class DTDConverter extends CommonTreeVisitor {

	static GrammarFile dtdGrammar = null;
	public final static GrammarFile loadGrammar(String filePath, NezOption option) throws IOException {
		if(dtdGrammar == null) {
			try {
				dtdGrammar = GrammarFile.loadGrammarFile("xmldtd.nez", NezOption.newSafeOption());
			}
			catch(IOException e) {
				ConsoleUtils.exit(1, "can't load xmldtd.nez");
			}
		}
		Grammar p = dtdGrammar.newGrammar("File");
		SourceContext dtdFile = SourceContext.newFileContext(filePath);
		CommonTree node = p.parse(dtdFile);
		if (node == null) {
			throw new NezException(dtdFile.getSyntaxErrorMessage());
		}
		if (dtdFile.hasUnconsumed()) {
			throw new NezException(dtdFile.getUnconsumedMessage());
		}
		DTDConverter conv = new DTDConverter();
		GrammarFile gfile = GrammarFile.newGrammarFile(filePath, option);
		conv.convert(node, gfile);
		gfile.verify();
		return gfile;
	}

	private GrammarFile gfile;
	
	DTDConverter() {
	}
	
	final void convert(CommonTree node, GrammarFile grammar) {
		this.gfile = grammar;
		this.loadPredfinedRules(node);
		this.visit("visit", node);
	}

	final void loadPredfinedRules(CommonTree node) {
		String rootElement = node.get(0).textAt(0, null);
		PredefinedRules preRules = new PredefinedRules(this.gfile, rootElement);
		preRules.defineRule();
	}
	
	
	int attID;
	int attDefCount = 0;
	int elementCount = 0;
	int entityCount = 0;
	
	Map<Integer, String> elementNameMap = new HashMap<Integer, String>();
	Map<String, Integer> attributeMap = new HashMap<String, Integer>();
	List<Integer> reqList;
	List<Integer> impList;

	
	final void initAttCounter() {
		attID = elementCount - 1;
		attDefCount = 0;
		reqList = new ArrayList<Integer>();
		impList = new ArrayList<Integer>();
	}

	final int[] initAttDefList() {
		int[] attDefList = new int[attDefCount];
		for (int i = 0; i < attDefList.length; i++) {
			attDefList[i] = i;
		}
		return attDefList;
	}

	public void visitDoc(CommonTree node) {
		for(CommonTree subnode: node) {
			this.visit("visit", subnode);
		}
		for (int elementID = 0; elementID < elementCount; elementID++) {
			String elementName = "Element_" + elementNameMap.get(elementID);
			gfile.defineProduction(node, elementName, genElement(node, elementID));
		}
		gfile.defineProduction(node, "Entity", genEntityList(node));
	}

	public void visitElement(CommonTree node) {
		String elementName = node.textAt(0, "");
		elementNameMap.put(elementCount, elementName);
		//		elementIDMap.put(elementName, elementCount);
		gfile.defineProduction(node, "Content" + elementCount, toExpression(node.get(1)));
		elementCount++;
	}

	private Expression genElement(CommonTree node, int elementID) {
		String elementName = elementNameMap.get(elementID);
		Expression[] contentSeq = {
				gfile.newByteChar('>'),
				gfile.newRepetition(gfile.newNonTerminal("S")),
				gfile.newNonTerminal("Content" + elementID),
				gfile.newRepetition(gfile.newNonTerminal("S")),
				gfile.newString("</" + elementName + ">"),
		};
		Expression[] attOnlySeq = {
			gfile.newString("/>"),
		};
		Expression[] endChoice = {
				gfile.newSequence(attOnlySeq), gfile.newSequence(contentSeq)
		};
		// check whether attribute exists
		if (attributeMap.containsValue(elementID)) {
			Expression[] l = {
					gfile.newString("<" + elementName),
					gfile.newRepetition(gfile.newNonTerminal("S")),
					gfile.newNonTerminal("Attribute" + elementID),
					gfile.newRepetition(gfile.newNonTerminal("S")),
					gfile.newChoice(endChoice),
					gfile.newRepetition(gfile.newNonTerminal("S")),
			};
			return gfile.newSequence(l);
		}
		else {
			Expression[] l = {
					gfile.newString("<" + elementName),
					gfile.newRepetition(gfile.newNonTerminal("S")),
					gfile.newChoice(endChoice),
					gfile.newRepetition(gfile.newNonTerminal("S")),

			};
			return gfile.newSequence(l);
		}
	}

	public void visitAttlist(CommonTree node) {
		initAttCounter();
		String elementName = node.textAt(0, "");
		attributeMap.put(elementName, attID);
		String attListName = "Attribute" + attID;
		String choiceListName = "AttChoice" + attID;
		for (int attNum = 1; attNum < node.size(); attNum++) {
			this.visit("visit", node.get(attNum));
		}
		int[] attDefList = initAttDefList();
		// generate Complete / Proximate Attribute list
		if (impList.isEmpty()) {
			gfile.defineProduction(node, attListName, genCompAtt(node, attDefList));
		} else {
			int[] requiredRules = extractRequiredRule(attDefList);
			gfile.defineProduction(node, choiceListName, genImpliedChoice(node));
			gfile.defineProduction(node, attListName, genProxAtt(node, requiredRules));
		}
	}

	public void visitREQUIRED(CommonTree node) {
		String name = "AttDef" + attID + "_" + attDefCount;
		reqList.add(attDefCount++);
		gfile.defineProduction(node, name, toExpression(node.get(1)));
	}

	public void visitIMPLIED(CommonTree node) {
		String name = "AttDef" + attID + "_" + attDefCount;
		impList.add(attDefCount++);
		gfile.defineProduction(node, name, toExpression(node.get(1)));
	}

	public void visitFIXED(CommonTree node) {
		String name = "AttDef" + attID + "_" + attDefCount;
		impList.add(attDefCount++);
		gfile.defineProduction(node, name, genFixedAtt(node));
	}


	public void visitDefault(CommonTree node) {
		String name = "AttDef" + attID + "_" + attDefCount;
		impList.add(attDefCount++);
		gfile.defineProduction(node, name, toExpression(node.get(1)));
	}

	public void visitEntity(CommonTree node) {
		String name = "ENT_" + entityCount++;
		gfile.defineProduction(node, name, toExpression(node.get(1)));
	}
	
	private Expression toExpression(CommonTree node) {
		return (Expression)this.visit("to", node);
	}
	
	public Expression toEmpty(CommonTree node) {
		return gfile.newNonTerminal("EMPTY");
	}

	public Expression toAny(CommonTree node) {
		return gfile.newNonTerminal("ANY");
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
		String attName = node.getParent().textAt(0, "");
		Expression[] l = {
				gfile.newString(attName),
				gfile.newRepetition(gfile.newNonTerminal("S")),
				gfile.newByteChar('='),
				gfile.newRepetition(gfile.newNonTerminal("S")),
				gfile.newNonTerminal("STRING"),
				gfile.newRepetition(gfile.newNonTerminal("S")),
		};
		return gfile.newSequence(l);
	}

	public Expression toID(CommonTree node) {
		String attName = node.getParent().textAt(0, "");
		Expression[] l = {
				gfile.newString(attName),
				gfile.newRepetition(gfile.newNonTerminal("S")),
				gfile.newByteChar('='),
				gfile.newRepetition(gfile.newNonTerminal("S")),
				gfile.newString("\""),
				gfile.newNonTerminal("IDTOKEN"),
				gfile.newString("\""),
				gfile.newRepetition(gfile.newNonTerminal("S")),
		//		grammar.newDefSymbol(Tag.tag("IDLIST"),
		//				grammar.newNonTerminal("IDTOKEN")));
		};
		return gfile.newSequence(l);

	}

	public Expression toIDREF(CommonTree node) {
		String attName = node.getParent().textAt(0, "");
		Expression[] l = {
				gfile.newString(attName),
				gfile.newRepetition(gfile.newNonTerminal("S")),
				gfile.newByteChar('='),
				gfile.newRepetition(gfile.newNonTerminal("S")),
				gfile.newString("\""),
				gfile.newNonTerminal("IDTOKEN"),
				gfile.newString("\""),
				gfile.newRepetition(gfile.newNonTerminal("S")),
		};
		return gfile.newSequence(l);
	}

	public Expression toIDREFS(CommonTree node) {
		String attName = node.getParent().textAt(0, "");
		Expression[] l = {
				gfile.newString(attName),
				gfile.newRepetition(gfile.newNonTerminal("S")),
				gfile.newByteChar('='),
				gfile.newRepetition(gfile.newNonTerminal("S")),
				gfile.newString("\""),
				gfile.newNonTerminal("IDTOKENS"),
				gfile.newString("\""),
				gfile.newRepetition(gfile.newNonTerminal("S")),
		//(grammar.newRepetition(node, grammar.newIsaSymbol(node, Tag.tag("IDLIST"))));
		};
		return gfile.newSequence(l);
	}

	private Expression genFixedAtt(CommonTree node) {
		String attName = node.textAt(0, "");
		String fixedValue = "\"" + node.textAt(2, "") + "\"";
		Expression[] l ={
				gfile.newString(attName),
				gfile.newRepetition(gfile.newNonTerminal("S")),
				gfile.newByteChar('='),
				gfile.newRepetition(gfile.newNonTerminal("S")),
				gfile.newString(fixedValue),
				gfile.newRepetition(gfile.newNonTerminal("S")),

		};
		return gfile.newSequence(l);
	}

	public Expression toENTITY(CommonTree node) {
		String attName = node.getParent().textAt(0, "");
		Expression[] l ={
				gfile.newString(attName),
				gfile.newRepetition(gfile.newNonTerminal("S")),
				gfile.newByteChar('='),
				gfile.newRepetition(gfile.newNonTerminal("S")),
				gfile.newString("\""),
				gfile.newNonTerminal("entity"),
				gfile.newString("\""),
				gfile.newRepetition(gfile.newNonTerminal("S")),
		};
		return gfile.newSequence(l);
	}

	public Expression toENTITIES(CommonTree node) {
		String attName = node.getParent().textAt(0, "");
		Expression[] l = {
				gfile.newString(attName),
				gfile.newRepetition(gfile.newNonTerminal("S")),
				gfile.newByteChar('='),
				gfile.newRepetition(gfile.newNonTerminal("S")),
				gfile.newString("\""),
				gfile.newNonTerminal("entities"),
				gfile.newString("\""),
				gfile.newRepetition(gfile.newNonTerminal("S")),
		};
		return gfile.newSequence(l);
	}

	public Expression toNMTOKEN(CommonTree node) {
		String attName = node.getParent().textAt(0, "");
		Expression[] l = {
				gfile.newString(attName),
				gfile.newRepetition(gfile.newNonTerminal("S")),
				gfile.newByteChar('='),
				gfile.newRepetition(gfile.newNonTerminal("S")),
				gfile.newNonTerminal("NMTOKEN"),
				gfile.newRepetition(gfile.newNonTerminal("S")),
		};
		return gfile.newSequence(l);
	}

	public Expression toNMTOKENS(CommonTree node) {
		String attName = node.getParent().textAt(0, "");
		Expression[] l = {
				gfile.newString(attName),
				gfile.newRepetition(gfile.newNonTerminal("S")),
				gfile.newByteChar('='),
				gfile.newRepetition(gfile.newNonTerminal("S")),
				gfile.newNonTerminal("NMTOKEN"),
				gfile.newRepetition(gfile.newNonTerminal("S")),
		};
		return gfile.newSequence(l);
	}
	
	public Expression genCompAtt(CommonTree node, int[] attlist) {
		int listLength = attlist.length;
		if (listLength == 1) {
			Expression[] l = {
					gfile.newNonTerminal("AttDef" + attID + "_" + attlist[0]),
					gfile.newRepetition(gfile.newNonTerminal("S")),
					gfile.newNonTerminal("ENDTAG")
			};
			return gfile.newSequence(l);
		} else {
			int[][] permutationList = perm(attlist);
			Expression[] choiceList = new Expression[permutationList.length];
			int choiceCount = 0;
			for (int[] target : permutationList) {
				Expression[] seqList = new Expression[listLength + 1];
				for (int index = 0; index < target.length; index++) {
					seqList[index] = gfile.newNonTerminal("AttDef" + attID + "_"
							+ target[index]);
				}
				seqList[listLength] = gfile.newNonTerminal("ENDTAG");
				choiceList[choiceCount++] = gfile.newSequence(seqList);
			}
			return gfile.newChoice(choiceList);
		}
	}

	public Expression genProxAtt(CommonTree node, int[] attlist) {
		int listLength = attlist.length;
		if (listLength == 0) {
			Expression[] l = {
					gfile.newRepetition(gfile.newNonTerminal("AttChoice" + attID)),
					gfile.newRepetition(gfile.newNonTerminal("S")),
					gfile.newNonTerminal("ENDTAG")
			};
			return gfile.newSequence(l);
		} else {
			int[][] permedList = perm(attlist);
			Expression[] choiceList = new Expression[permedList.length];
			int choiceCount = 0;
			for (int[] target : permedList) {
				Expression[] seqList = new Expression[listLength * 2 + 2];
				int seqCount = 0;
				seqList[seqCount++] = gfile
						.newRepetition(gfile.newNonTerminal("AttChoice" + attID));
				for (int index = 0; index < target.length; index++) {
					seqList[seqCount++] = gfile.newNonTerminal("AttDef" + attID + "_"
							+ target[index]);
					seqList[seqCount++] = gfile.newRepetition(gfile.newNonTerminal("AttChoice"
							+ attID));
				}
				seqList[seqCount] = gfile.newNonTerminal("ENDTAG");
				choiceList[choiceCount++] = gfile.newSequence(seqList);
			}
			return gfile.newChoice(choiceList);
		}
	}

	public Expression genImpliedChoice(CommonTree node){
		Expression[] l = new Expression[impList.size()];
		String definitionName = "AttDef" + attID + "_";
		int choiceCount = 0;
		for (Integer ruleNum : impList) {
			l[choiceCount++] = gfile.newNonTerminal(definitionName + ruleNum);
		}
		return gfile.newChoice(l);
	}
	

	public Expression toEnum(CommonTree node) {
		String attName = node.getParent().textAt(0, "");
		Expression[] l = {
				gfile.newString(attName),
				gfile.newRepetition(gfile.newNonTerminal("S")),
				gfile.newByteChar('='),
				gfile.newRepetition(gfile.newNonTerminal("S")),
				gfile.newByteChar('"'),
				toChoice(node),
				gfile.newByteChar('"'),
				gfile.newRepetition(gfile.newNonTerminal("S")),
		};
		return gfile.newSequence(l);
	}

	public Expression toEntValue(CommonTree node) {
		String replaceString = node.getText();
		return gfile.newString(replaceString);
	}

	public Expression toElName(CommonTree node) {
		String elementName = "Element_" + node.getText();
		return gfile.newNonTerminal(elementName);
	}

	public Expression toName(CommonTree node) {
		return gfile.newString(node.getText());
	}

	public Expression toData(CommonTree node) {
		return gfile.newNonTerminal("PCDATA");
	}

	public Expression toOnlyData(CommonTree node) {
		return gfile.newRepetition(gfile.newNonTerminal("PCDATA"));
	}

	private Expression genEntityList(CommonTree node) {
		if (entityCount == 0) {
			return GrammarFactory.newFailure(null);
		}
		else {
			Expression[] l = new Expression[entityCount];
			for (int entityNum = 0; entityNum < entityCount; entityNum++) {
				l[entityNum] = gfile.newNonTerminal("ENT_" + entityNum);
			}
			return gfile.newChoice(l);
		}
	}

	private final int[] extractRequiredRule(int[] attlist) {
		int[] buf = new int[512];
		int arrIndex = 0;
		for (int requiredNum : attlist) {
			if (reqList.contains(requiredNum)) {
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

}


