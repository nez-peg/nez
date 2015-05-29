package nez.peg.dtd;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nez.NezException;
import nez.SourceContext;
import nez.ast.CommonTree;
import nez.ast.CommonTreeVisitor;
import nez.lang.Expression;
import nez.lang.Grammar;
import nez.lang.GrammarChecker;
import nez.lang.GrammarFactory;
import nez.lang.NameSpace;
import nez.util.ConsoleUtils;

public class DTDConverter extends CommonTreeVisitor {

	static NameSpace dtdGrammar = null;
	public final static NameSpace loadGrammar(String filePath, GrammarChecker checker) throws IOException {
		if(dtdGrammar == null) {
			try {
				dtdGrammar = NameSpace.loadGrammarFile("xmldtd.nez");
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
		NameSpace grammar = NameSpace.newNameSpace(filePath);
		conv.convert(node, grammar);
		checker.verify(grammar);
		return grammar;
	}

	private NameSpace grammar;
	
	DTDConverter() {
	}
	
	final void convert(CommonTree node, NameSpace grammar) {
		this.grammar = grammar;
		this.loadPredfinedRules(node);
		this.visit("visit", node);
	}

	final void loadPredfinedRules(CommonTree node) {
		String rootElement = node.get(0).textAt(0, null);
		PredefinedRules preRules = new PredefinedRules(this.grammar, rootElement);
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
			grammar.defineProduction(node, elementName, genElement(node, elementID));
		}
		grammar.defineProduction(node, "Entity", genEntityList(node));
	}

	public void visitElement(CommonTree node) {
		String elementName = node.textAt(0, "");
		elementNameMap.put(elementCount, elementName);
		//		elementIDMap.put(elementName, elementCount);
		grammar.defineProduction(node, "Content" + elementCount, toExpression(node.get(1)));
		elementCount++;
	}

	private Expression genElement(CommonTree node, int elementID) {
		String elementName = elementNameMap.get(elementID);
		Expression[] contentSeq = {
				grammar.newByteChar('>'),
				grammar.newRepetition(grammar.newNonTerminal("S")),
				grammar.newNonTerminal("Content" + elementID),
				grammar.newRepetition(grammar.newNonTerminal("S")),
				grammar.newString("</" + elementName + ">"),
		};
		Expression[] attOnlySeq = {
			grammar.newString("/>"),
		};
		Expression[] endChoice = {
				grammar.newSequence(attOnlySeq), grammar.newSequence(contentSeq)
		};
		// check whether attribute exists
		if (attributeMap.containsValue(elementID)) {
			Expression[] l = {
					grammar.newString("<" + elementName),
					grammar.newRepetition(grammar.newNonTerminal("S")),
					grammar.newNonTerminal("Attribute" + elementID),
					grammar.newRepetition(grammar.newNonTerminal("S")),
					grammar.newChoice(endChoice),
					grammar.newRepetition(grammar.newNonTerminal("S")),
			};
			return grammar.newSequence(l);
		}
		else {
			Expression[] l = {
					grammar.newString("<" + elementName),
					grammar.newRepetition(grammar.newNonTerminal("S")),
					grammar.newChoice(endChoice),
					grammar.newRepetition(grammar.newNonTerminal("S")),

			};
			return grammar.newSequence(l);
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
			grammar.defineProduction(node, attListName, genCompAtt(node, attDefList));
		} else {
			int[] requiredRules = extractRequiredRule(attDefList);
			grammar.defineProduction(node, choiceListName, genImpliedChoice(node));
			grammar.defineProduction(node, attListName, genProxAtt(node, requiredRules));
		}
	}

	public void visitREQUIRED(CommonTree node) {
		String name = "AttDef" + attID + "_" + attDefCount;
		reqList.add(attDefCount++);
		grammar.defineProduction(node, name, toExpression(node.get(1)));
	}

	public void visitIMPLIED(CommonTree node) {
		String name = "AttDef" + attID + "_" + attDefCount;
		impList.add(attDefCount++);
		grammar.defineProduction(node, name, toExpression(node.get(1)));
	}

	public void visitFIXED(CommonTree node) {
		String name = "AttDef" + attID + "_" + attDefCount;
		impList.add(attDefCount++);
		grammar.defineProduction(node, name, genFixedAtt(node));
	}


	public void visitDefault(CommonTree node) {
		String name = "AttDef" + attID + "_" + attDefCount;
		impList.add(attDefCount++);
		grammar.defineProduction(node, name, toExpression(node.get(1)));
	}

	public void visitEntity(CommonTree node) {
		String name = "ENT_" + entityCount++;
		grammar.defineProduction(node, name, toExpression(node.get(1)));
	}
	
	private Expression toExpression(CommonTree node) {
		return (Expression)this.visit("to", node);
	}
	
	public Expression toEmpty(CommonTree node) {
		return grammar.newNonTerminal("EMPTY");
	}

	public Expression toAny(CommonTree node) {
		return grammar.newNonTerminal("ANY");
	}

	public Expression toZeroMore(CommonTree node) {
		return grammar.newRepetition(toExpression(node.get(0)));
	}

	public Expression toOneMore(CommonTree node) {
		return grammar.newRepetition1(toExpression(node.get(0)));
	}

	public Expression toOption(CommonTree node) {
		return grammar.newOption(toExpression(node.get(0)));
	}

	public Expression toChoice(CommonTree node) {
		Expression[] l = new Expression[node.size()];
		int count = 0;
		for (CommonTree subnode : node) {
			l[count++] = toExpression(subnode);
		}
		return grammar.newChoice(l);
	}

	public Expression toSeq(CommonTree node) {
		Expression[] l = new Expression[node.size()];
		int count = 0;
		for (CommonTree subnode : node) {
			l[count++] = toExpression(subnode);
		}
		return grammar.newSequence(l);
	}

	public Expression toCDATA(CommonTree node) {
		String attName = node.getParent().textAt(0, "");
		Expression[] l = {
				grammar.newString(attName),
				grammar.newRepetition(grammar.newNonTerminal("S")),
				grammar.newByteChar('='),
				grammar.newRepetition(grammar.newNonTerminal("S")),
				grammar.newNonTerminal("STRING"),
				grammar.newRepetition(grammar.newNonTerminal("S")),
		};
		return grammar.newSequence(l);
	}

	public Expression toID(CommonTree node) {
		String attName = node.getParent().textAt(0, "");
		Expression[] l = {
				grammar.newString(attName),
				grammar.newRepetition(grammar.newNonTerminal("S")),
				grammar.newByteChar('='),
				grammar.newRepetition(grammar.newNonTerminal("S")),
				grammar.newString("\""),
				grammar.newNonTerminal("IDTOKEN"),
				grammar.newString("\""),
				grammar.newRepetition(grammar.newNonTerminal("S")),
		//		grammar.newDefSymbol(Tag.tag("IDLIST"),
		//				grammar.newNonTerminal("IDTOKEN")));
		};
		return grammar.newSequence(l);

	}

	public Expression toIDREF(CommonTree node) {
		String attName = node.getParent().textAt(0, "");
		Expression[] l = {
				grammar.newString(attName),
				grammar.newRepetition(grammar.newNonTerminal("S")),
				grammar.newByteChar('='),
				grammar.newRepetition(grammar.newNonTerminal("S")),
				grammar.newString("\""),
				grammar.newNonTerminal("IDTOKEN"),
				grammar.newString("\""),
				grammar.newRepetition(grammar.newNonTerminal("S")),
		};
		return grammar.newSequence(l);
	}

	public Expression toIDREFS(CommonTree node) {
		String attName = node.getParent().textAt(0, "");
		Expression[] l = {
				grammar.newString(attName),
				grammar.newRepetition(grammar.newNonTerminal("S")),
				grammar.newByteChar('='),
				grammar.newRepetition(grammar.newNonTerminal("S")),
				grammar.newString("\""),
				grammar.newNonTerminal("IDTOKENS"),
				grammar.newString("\""),
				grammar.newRepetition(grammar.newNonTerminal("S")),
		//(grammar.newRepetition(node, grammar.newIsaSymbol(node, Tag.tag("IDLIST"))));
		};
		return grammar.newSequence(l);
	}

	private Expression genFixedAtt(CommonTree node) {
		String attName = node.textAt(0, "");
		String fixedValue = "\"" + node.textAt(2, "") + "\"";
		Expression[] l ={
				grammar.newString(attName),
				grammar.newRepetition(grammar.newNonTerminal("S")),
				grammar.newByteChar('='),
				grammar.newRepetition(grammar.newNonTerminal("S")),
				grammar.newString(fixedValue),
				grammar.newRepetition(grammar.newNonTerminal("S")),

		};
		return grammar.newSequence(l);
	}

	public Expression toENTITY(CommonTree node) {
		String attName = node.getParent().textAt(0, "");
		Expression[] l ={
				grammar.newString(attName),
				grammar.newRepetition(grammar.newNonTerminal("S")),
				grammar.newByteChar('='),
				grammar.newRepetition(grammar.newNonTerminal("S")),
				grammar.newString("\""),
				grammar.newNonTerminal("entity"),
				grammar.newString("\""),
				grammar.newRepetition(grammar.newNonTerminal("S")),
		};
		return grammar.newSequence(l);
	}

	public Expression toENTITIES(CommonTree node) {
		String attName = node.getParent().textAt(0, "");
		Expression[] l = {
				grammar.newString(attName),
				grammar.newRepetition(grammar.newNonTerminal("S")),
				grammar.newByteChar('='),
				grammar.newRepetition(grammar.newNonTerminal("S")),
				grammar.newString("\""),
				grammar.newNonTerminal("entities"),
				grammar.newString("\""),
				grammar.newRepetition(grammar.newNonTerminal("S")),
		};
		return grammar.newSequence(l);
	}

	public Expression toNMTOKEN(CommonTree node) {
		String attName = node.getParent().textAt(0, "");
		Expression[] l = {
				grammar.newString(attName),
				grammar.newRepetition(grammar.newNonTerminal("S")),
				grammar.newByteChar('='),
				grammar.newRepetition(grammar.newNonTerminal("S")),
				grammar.newNonTerminal("NMTOKEN"),
				grammar.newRepetition(grammar.newNonTerminal("S")),
		};
		return grammar.newSequence(l);
	}

	public Expression toNMTOKENS(CommonTree node) {
		String attName = node.getParent().textAt(0, "");
		Expression[] l = {
				grammar.newString(attName),
				grammar.newRepetition(grammar.newNonTerminal("S")),
				grammar.newByteChar('='),
				grammar.newRepetition(grammar.newNonTerminal("S")),
				grammar.newNonTerminal("NMTOKEN"),
				grammar.newRepetition(grammar.newNonTerminal("S")),
		};
		return grammar.newSequence(l);
	}
	
	public Expression genCompAtt(CommonTree node, int[] attlist) {
		int listLength = attlist.length;
		if (listLength == 1) {
			Expression[] l = {
					grammar.newNonTerminal("AttDef" + attID + "_" + attlist[0]),
					grammar.newRepetition(grammar.newNonTerminal("S")),
					grammar.newNonTerminal("ENDTAG")
			};
			return grammar.newSequence(l);
		} else {
			int[][] permutationList = perm(attlist);
			Expression[] choiceList = new Expression[permutationList.length];
			int choiceCount = 0;
			for (int[] target : permutationList) {
				Expression[] seqList = new Expression[listLength + 1];
				for (int index = 0; index < target.length; index++) {
					seqList[index] = grammar.newNonTerminal("AttDef" + attID + "_"
							+ target[index]);
				}
				seqList[listLength] = grammar.newNonTerminal("ENDTAG");
				choiceList[choiceCount++] = grammar.newSequence(seqList);
			}
			return grammar.newChoice(choiceList);
		}
	}

	public Expression genProxAtt(CommonTree node, int[] attlist) {
		int listLength = attlist.length;
		if (listLength == 0) {
			Expression[] l = {
					grammar.newRepetition(grammar.newNonTerminal("AttChoice" + attID)),
					grammar.newRepetition(grammar.newNonTerminal("S")),
					grammar.newNonTerminal("ENDTAG")
			};
			return grammar.newSequence(l);
		} else {
			int[][] permedList = perm(attlist);
			Expression[] choiceList = new Expression[permedList.length];
			int choiceCount = 0;
			for (int[] target : permedList) {
				Expression[] seqList = new Expression[listLength * 2 + 2];
				int seqCount = 0;
				seqList[seqCount++] = grammar
						.newRepetition(grammar.newNonTerminal("AttChoice" + attID));
				for (int index = 0; index < target.length; index++) {
					seqList[seqCount++] = grammar.newNonTerminal("AttDef" + attID + "_"
							+ target[index]);
					seqList[seqCount++] = grammar.newRepetition(grammar.newNonTerminal("AttChoice"
							+ attID));
				}
				seqList[seqCount] = grammar.newNonTerminal("ENDTAG");
				choiceList[choiceCount++] = grammar.newSequence(seqList);
			}
			return grammar.newChoice(choiceList);
		}
	}

	public Expression genImpliedChoice(CommonTree node){
		Expression[] l = new Expression[impList.size()];
		String definitionName = "AttDef" + attID + "_";
		int choiceCount = 0;
		for (Integer ruleNum : impList) {
			l[choiceCount++] = grammar.newNonTerminal(definitionName + ruleNum);
		}
		return grammar.newChoice(l);
	}
	

	public Expression toEnum(CommonTree node) {
		String attName = node.getParent().textAt(0, "");
		Expression[] l = {
				grammar.newString(attName),
				grammar.newRepetition(grammar.newNonTerminal("S")),
				grammar.newByteChar('='),
				grammar.newRepetition(grammar.newNonTerminal("S")),
				grammar.newByteChar('"'),
				toChoice(node),
				grammar.newByteChar('"'),
				grammar.newRepetition(grammar.newNonTerminal("S")),
		};
		return grammar.newSequence(l);
	}

	public Expression toEntValue(CommonTree node) {
		String replaceString = node.getText();
		return grammar.newString(replaceString);
	}

	public Expression toElName(CommonTree node) {
		String elementName = "Element_" + node.getText();
		return grammar.newNonTerminal(elementName);
	}

	public Expression toName(CommonTree node) {
		return grammar.newString(node.getText());
	}

	public Expression toData(CommonTree node) {
		return grammar.newNonTerminal("PCDATA");
	}

	public Expression toOnlyData(CommonTree node) {
		return grammar.newRepetition(grammar.newNonTerminal("PCDATA"));
	}

	private Expression genEntityList(CommonTree node) {
		if (entityCount == 0) {
			return GrammarFactory.newFailure(null);
		}
		else {
			Expression[] l = new Expression[entityCount];
			for (int entityNum = 0; entityNum < entityCount; entityNum++) {
				l[entityNum] = grammar.newNonTerminal("ENT_" + entityNum);
			}
			return grammar.newChoice(l);
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


