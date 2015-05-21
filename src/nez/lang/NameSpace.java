package nez.lang;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import nez.ParserCombinator;
import nez.SourceContext;
import nez.ast.SourcePosition;
import nez.ast.Tag;
import nez.main.Verbose;
import nez.peg.dtd.DTDConverter;
import nez.util.ConsoleUtils;
import nez.util.UList;
import nez.util.UMap;

public class NameSpace {
	private static int nsid = 0;
	private static HashMap<String, NameSpace> nsMap = new HashMap<String, NameSpace>();

	public final static boolean isLoaded(String urn) {
		return nsMap.containsKey(urn);
	}

	public static NameSpace newNameSpace() {
		return new NameSpace(nsid++, null);
	}

	public final static NameSpace newNameSpace(String urn) {
		if(urn != null && nsMap.containsKey(urn)) {
			return nsMap.get(urn);
		}
		NameSpace ns = new NameSpace(nsid++, urn);
		if(urn != null) {
			nsMap.put(urn, ns);
		}
		return ns;
	}

	public final static NameSpace loadNezFile(String urn, GrammarChecker checker) throws IOException {
		if(nsMap.containsKey(urn)) {
			return nsMap.get(urn);
		}
		NameSpace ns = null;
		if(urn != null && !urn.endsWith(".nez")) {
			try {
				Class<?> c = Class.forName(urn);
				ParserCombinator p = (ParserCombinator)c.newInstance();
				 ns = p.load();
			}
			catch(ClassNotFoundException e) {
			}
			catch(Exception e) {
				Verbose.traceException(e);
			}
		}
		if(ns == null) {
			ns = new NameSpace(nsid++, urn);
			NezParser parser = new NezParser();
			parser.load(ns, urn, checker);
		}
		nsMap.put(urn, ns);
		return ns;
	}

	public final static NameSpace loadNezFile(String urn) throws IOException {
		return loadNezFile(urn, new GrammarChecker());
	}
	
	public final static NameSpace loadGrammarFile(String urn, GrammarChecker checker) throws IOException {
		if(urn.endsWith(".dtd")) {
			return DTDConverter.loadGrammar(urn, checker);
		}
		return loadNezFile(urn, checker);
	}

	public final static NameSpace loadGrammarFile(String file) throws IOException {
		return loadGrammarFile(file, new GrammarChecker());
	}

	public final static String nameUniqueName(String ns, String name) {
		return ns + ":" + name;
	}
	
	public final static String nameNamespaceName(String ns, String name) {
		return ns == null ? name :  ns + "." + name;
	}

	public final static String nameTerminalProduction(String t) {
		return "\"" + t + "\"";
	}

	// static 
	
	final int             id;
	final String          urn;
	final String          ns;
	final UMap<Production>      ruleMap;
	final UList<String>   nameList;

	private NameSpace(int id, String urn) {
		this.id = id;
		this.urn = urn;
		String ns = "g";
		if(urn != null) {
			int loc = urn.lastIndexOf('/');
			if(loc != -1) {
				ns = urn.substring(loc+1);
			}
			ns = ns.replace(".nez", "");
		}
		this.ns = ns;
		this.ruleMap = new UMap<Production>();
		this.nameList = new UList<String>(new String[8]);
	}

	public final String uniqueName(String localName) {
		return this.ns + this.id + ":" + localName;
	}

	public final String getURN() {
		return this.urn;
	}
	
	public final boolean hasProduction(String localName) {
		return this.ruleMap.get(localName) != null;
	}

	public final void addProduction(Production p) {
		this.ruleMap.put(p.getUniqueName(), p);
	}

	public final Production defineProduction(SourcePosition s, String localName, Expression e) {
		if(!hasProduction(localName)) {
			nameList.add(localName);
		}
		Production p = new Production(s, this, localName, e);
		this.ruleMap.put(localName, p);
		addProduction(p);
		return p;
	}

	public final Production inportProduction(String ns, Production p) {
		this.ruleMap.put(nameNamespaceName(ns, p.getLocalName()), p);
		addProduction(p);
		return p;
	}
	
	public final Production getProduction(String ruleName) {
		return this.ruleMap.get(ruleName);
	}
	
	public final List<String> getNonterminalList() {
		ArrayList<String> l = new ArrayList<String>();
		for(String s : this.ruleMap.keys()) {
			if(s.indexOf(':') > 0) continue;
			char c = s.charAt(0);
			if(!Character.isUpperCase(c)) {
				continue;
			}
			l.add(s);
		}
		Collections.sort(l);
		return l;
	}
	
	public Production newReducedProduction(String localName, Production p, Manipulator m) {
		Production r = p.newProduction(localName);
		this.ruleMap.put(localName, r);
		m.updateProductionAttribute(p, r);
		addProduction(r);
		r.setExpression(p.getExpression().reshape(m));
		return r;
	}
	
		
//	public int getRuleSize() {
//		return this.ruleMap.size();
//	}



	public final Production newRule(String name, Expression e) {
		Production r = new Production(null, this, name, e);
		this.ruleMap.put(name, r);
		return r;
	}

	public final UList<Production> getDefinedRuleList() {
		UList<Production> ruleList = new UList<Production>(new Production[this.nameList.size()]);
		for(String n : nameList) {
			ruleList.add(this.getProduction(n));
		}
		return ruleList;
	}

	public final UList<Production> getRuleList() {
		UList<Production> ruleList = new UList<Production>(new Production[this.ruleMap.size()]);
		for(String n : this.ruleMap.keys()) {
			ruleList.add(this.getProduction(n));
		}
		return ruleList;
	}

	public final Grammar newGrammar(String name, int option) {
		Production r = this.getProduction(name);
		if(r != null) {
			return new Grammar(r, option);
		}
		//System.out.println("** " + this.ruleMap.keys());
		return null;
	}

	public final Grammar newGrammar(String name) {
		return this.newGrammar(name, Grammar.DefaultOption);
	}

	public void dump() {
		for(Production r : this.getRuleList()) {
			ConsoleUtils.println(r);
		}
	}
	
	private Map<String, Expression> tableMap; 

	final void setSymbolExpresion(String tableName, Expression e) {
		if(tableMap == null) {
			tableMap = new HashMap<String, Expression>();
		}
		tableMap.put(tableName, e);
	}

	final Expression getSymbolExpresion(String tableName) {
		if(tableMap != null) {
			Expression e = tableMap.get(tableName);
			if(e != null && !e.isInterned()) {
				e = e.intern();
				tableMap.put(tableName, e);
			}
			return e;
		}
		return null;
	}

	private FormatterMap fmtMap;
	
	public final void addFormatter(String tag, int size, Formatter fmt) {
		if(fmtMap == null) {
			fmtMap = new FormatterMap();
		}
		fmtMap.set(tag, size, fmt);
	}

	public final Formatter getFormatter(String tag, int size) {
		if(fmtMap != null) {
			return fmtMap.get(tag, size);
		}
		return null;
	}
	
	private UList<Example> exampleList;
	
	final void addExample(Example ex) {
		if(exampleList == null) {
			exampleList = new UList<Example>(new Example[2]);
		}
		exampleList.add(ex);
	}
	
	final void testExample(int option) {
		if(exampleList != null) {
			long t1 = System.nanoTime();
			for(Example ex : exampleList) {
				ex.test(this, option);
			}
			long t2 = System.nanoTime();
			if(Verbose.Example) {
				Verbose.println("Elapsed time (Example Tests): " + ((t2 - t1) / 1000000) + "ms"); 
			}
		}
	}

	// Grammar
	
	private SourcePosition src() {
		return null; // TODO
	}
	
	public final Expression newNonTerminal(String name) {
		return Factory.newNonTerminal(src(), this, name);
	}
	
	public final Expression newEmpty() {
		return Factory.newEmpty(src());
	}

	public final Expression newFailure() {
		return Factory.newFailure(src());
	}

	public final Expression newByteChar(int ch) {
		return Factory.newByteChar(src(), ch);
	}
	
	public final Expression newAnyChar() {
		return Factory.newAnyChar(src());
	}
	
	public final Expression newString(String text) {
		return Factory.newString(src(), text);
	}
	
	public final Expression newCharSet(SourcePosition s, String text) {
		return Factory.newCharSet(src(), text);
	}

	public final Expression newByteMap(boolean[] byteMap) {
		return Factory.newByteMap(src(), byteMap);
	}
	
	public final Expression newSequence(Expression ... seq) {
		UList<Expression> l = new UList<Expression>(new Expression[8]);
		for(Expression p: seq) {
			Factory.addSequence(l, p);
		}
		return Factory.newSequence(src(), l);
	}

	public final Expression newChoice(Expression ... seq) {
		UList<Expression> l = new UList<Expression>(new Expression[8]);
		for(Expression p: seq) {
			Factory.addChoice(l, p);
		}
		return Factory.newChoice(src(), l);
	}

	public final Expression newOption(Expression ... seq) {
		return Factory.newOption(src(), newSequence(seq));
	}
		
	public final Expression newRepetition(Expression ... seq) {
		return Factory.newRepetition(src(), newSequence(seq));
	}

	public final Expression newRepetition1(Expression ... seq) {
		return Factory.newRepetition1(src(), newSequence(seq));
	}

	public final Expression newAnd(Expression ... seq) {
		return Factory.newAnd(src(), newSequence(seq));
	}

	public final Expression newNot(Expression ... seq) {
		return Factory.newNot(src(), newSequence(seq));
	}
	
//	public final Expression newByteRange(int c, int c2) {
//		if(c == c2) {
//			return newByteChar(s, c);
//		}
//		return internImpl(s, new ByteMap(s, c, c2));
//	}
	
	// PEG4d
	public final Expression newMatch(Expression ... seq) {
		return Factory.newMatch(src(), newSequence(seq));
	}
	
	public final Expression newLink(Expression ... seq) {
		return Factory.newLink(src(), newSequence(seq), -1);
	}

	public final Expression newLink(int index, Expression ... seq) {
		return Factory.newLink(src(), newSequence(seq), index);
	}

	public final Expression newNew(Expression ... seq) {
		return Factory.newNew(src(), false, newSequence(seq));
	}

	public final Expression newLeftNew(Expression ... seq) {
		return Factory.newNew(src(), true, newSequence(seq));
	}

	public final Expression newTagging(String tag) {
		return Factory.newTagging(src(), Tag.tag(tag));
	}

	public final Expression newReplace(String msg) {
		return Factory.newReplace(src(), msg);
	}
	
	// Conditional Parsing
	// <if FLAG>
	// <on FLAG e>
	// <on !FLAG e>
	
	public final Expression newIfFlag(String flagName) {
		return Factory.newIfFlag(src(), flagName);
	}

	public final Expression newOnFlag(String flagName, Expression ... seq) {
		return Factory.newOnFlag(src(), true, flagName, newSequence(seq));
	}

	
	
	public final Expression newScan(SourcePosition s, int number, Expression scan, Expression repeat) {
		return null;
	}
	
	public final Expression newRepeat(SourcePosition s, Expression e) {
		return null;
	}
	
	public final Expression newBlock(Expression ... seq) {
		return Factory.newBlock(src(), newSequence(seq));
	}

	public final Expression newDefSymbol(SourcePosition s, String table, Expression ... seq) {
		return Factory.newDefSymbol(src(), this, Tag.tag(table), newSequence(seq));
	}

	public final Expression newIsSymbol(SourcePosition s, String table) {
		return Factory.newIsSymbol(src(), this, Tag.tag(table));
	}
	
	public final Expression newIsaSymbol(SourcePosition s, String table) {
		return Factory.newIsaSymbol(src(), this, Tag.tag(table));
	}

	public final Expression newExists(SourcePosition s, String table) {
		return Factory.newExists(src(), this, Tag.tag(table));
	}

	public final Expression newLocal(SourcePosition s, String table, Expression ... seq) {
		return Factory.newLocal(src(), this, Tag.tag(table), newSequence(seq));
	}

	public final Expression newDefIndent(SourcePosition s) {
		return Factory.newDefIndent(src());
	}

	public final Expression newIndent(SourcePosition s) {
		return Factory.newIndent(src());
	}

	// reporting errors
	
	boolean strictMode = true;
	
	public final void reportError(Expression p, String message) {
		if(p.s != null) {
			ConsoleUtils.println(p.s.formatSourceMessage("error", message));
		}
	}

	public final void reportWarning(Expression p, String message) {
		if(p.s != null) {
			ConsoleUtils.println(p.s.formatSourceMessage("warning", message));
		}
	}

	public final void reportNotice(Expression p, String message) {
		if(this.strictMode) {
			if(p.s != null) {
				ConsoleUtils.println(p.s.formatSourceMessage("notice", message));
			}
		}
	}



}
