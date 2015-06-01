package nez.lang;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nez.ParserCombinator;
import nez.ast.SourcePosition;
import nez.main.Verbose;
import nez.peg.celery.CeleryConverter;
import nez.peg.dtd.DTDConverter;
import nez.util.ConsoleUtils;
import nez.util.UList;
import nez.util.UMap;

public class NameSpace extends GrammarFactory {
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
		if (urn.endsWith(".cl")) {
			return CeleryConverter.loadGrammar(urn, checker);
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
	
	public Production newReducedProduction(String localName, Production p, GrammarReshaper m) {
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

	protected NameSpace getNameSpace() {
		return this;
	}

	protected SourcePosition getSourcePosition() {
		return null;
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
