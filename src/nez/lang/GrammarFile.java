package nez.lang;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nez.NezOption;
import nez.ParserCombinator;
import nez.ast.CommonTree;
import nez.ast.SourcePosition;
import nez.main.Command;
import nez.main.Verbose;
import nez.peg.celery.CeleryConverter;
import nez.peg.dtd.DTDConverter;
import nez.util.ConsoleUtils;
import nez.util.UList;
import nez.util.UMap;

public class GrammarFile extends GrammarFactory {
	
	private static int nsid = 0;
	private static HashMap<String, GrammarFile> nsMap = new HashMap<String, GrammarFile>();
	
	public final static boolean isLoaded(String urn) {
		return nsMap.containsKey(urn);
	}

	public static GrammarFile newGrammarFile(NezOption option) {
		return new GrammarFile(nsid++, null, option);
	}

	public final static GrammarFile newGrammarFile(String urn, NezOption option) {
		if(urn != null && nsMap.containsKey(urn)) {
			return nsMap.get(urn);
		}
		GrammarFile ns = new GrammarFile(nsid++, urn, option);
		if(urn != null) {
			nsMap.put(urn, ns);
		}
		return ns;
	}

	public final static GrammarFile loadNezFile(String urn, NezOption option) throws IOException {
		if(nsMap.containsKey(urn)) {
			return nsMap.get(urn);
		}
		GrammarFile ns = null;
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
			ns = new GrammarFile(nsid++, urn, option);
			NezParser parser = new NezParser();
			parser.load(ns, urn);
		}
		nsMap.put(urn, ns);
		return ns;
	}
	
	public final static GrammarFile loadGrammarFile(String urn, NezOption option) throws IOException {
		if(urn.endsWith(".dtd")) {
			return DTDConverter.loadGrammar(urn, option);
		}
		if (urn.endsWith(".cl")) {
			return CeleryConverter.loadGrammar(urn, option);
		}
		return loadNezFile(urn, option);
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

	// fields 
	
	final int               id;
	final String            urn;
	final String            ns;
	final UMap<Production>  ruleMap;
	final UList<String>     nameList;
	final NezOption     option;

	private GrammarFile(int id, String urn, NezOption option) {
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
		this.option = option;
	}

	public final String uniqueName(String localName) {
		return this.ns + this.id + ":" + localName;
	}

	public final String getURN() {
		return this.urn;
	}
	
	public final boolean isEmpty() {
		return this.ruleMap.size() == 0;
	}

	public final boolean hasProduction(String localName) {
		return this.ruleMap.get(localName) != null;
	}

	public final void addProduction(Production p) {
		this.ruleMap.put(p.getUniqueName(), p);
	}

	public final Production defineProduction(SourcePosition s, String localName, Expression e) {
		return this.defineProduction(s, 0, localName, e);
	}

	public final Production defineProduction(SourcePosition s, int flag, String localName, Expression e) {
		if(!hasProduction(localName)) {
			nameList.add(localName);
		}
		Production p = new Production(s, flag, this, localName, e);
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
	
	public final Production newProduction(int flag, String name, Expression e) {
		Production r = new Production(null, flag, this, name, e);
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

	public final Grammar newGrammar(String name, NezOption option) {
		Production r = this.getProduction(name);
		if(r != null) {
			return new Grammar(r, option);
		}
		//System.out.println("** " + this.ruleMap.keys());
		return null;
	}

	public final Grammar newGrammar(String name) {
		return this.newGrammar(name, NezOption.newDefaultOption());
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
	
	public final String formatCommonTree(CommonTree node) {
		return Formatter.format(this, node);
	}

	
	private UList<Example> exampleList;
	
	final void addExample(Example ex) {
		if(exampleList == null) {
			exampleList = new UList<Example>(new Example[2]);
		}
		exampleList.add(ex);
	}
	
	final void testExample(NezOption option) {
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

	protected GrammarFile getGrammarFile() {
		return this;
	}

	protected SourcePosition getSourcePosition() {
		return null;
	}
	

	// reporting errors
	
	boolean strictMode = true;
	
	public final void reportError(Expression p, String message) {
		this.reportError(p.getSourcePosition(), message);
	}

	public final void reportError(SourcePosition s, String message) {
		if(s != null) {
			ConsoleUtils.println(s.formatSourceMessage("error", message));
		}
	}

	public final void reportWarning(Expression p, String message) {
		this.reportWarning(p.getSourcePosition(), message);
	}

	public final void reportWarning(SourcePosition s, String message) {
		if(s != null) {
			ConsoleUtils.println(s.formatSourceMessage("warning", message));
		}
	}

	public final void reportNotice(Expression p, String message) {
		this.reportNotice(p.getSourcePosition(), message);
	}

	public final void reportNotice(SourcePosition s, String message) {
		if(this.strictMode) {
			if(s != null) {
				ConsoleUtils.println(s.formatSourceMessage("notice", message));
			}
		}
	}

	public void verify() {
		NameAnalysis nameAnalyzer = new NameAnalysis();
		nameAnalyzer.analyze(this.getDefinedRuleList());
//		if(this.foundError) {
//			ConsoleUtils.exit(1, "FatalGrammarError");
//		}
		// type check
		for(Production p: this.getRuleList()) {
			if(p.isTerminal()) {
				continue;
			}
			p.reshape(new Typestate(this));
		}		
		// interning
//		if(this.option == NezOption.DebugOption) {
//			for(Production r: grammar.getRuleList()) {
//				GrammarFactory.setId(r.getExpression());
//			}
//		}
//		else {
		for(Production r: this.getRuleList()) {
			if(r.isTerminal()) {
				continue;
			}
			if(Verbose.Grammar) {
				r.dump();
			}
			if(Command.ReleasePreview) {
				boolean r1 = r.isConditional();
				boolean r2 = r.testCondition(r.getExpression(), null);
				if(r1 != r2) {
					Verbose.FIXME("mismatch condition: " + r.getLocalName() + " " + r1 + " " + r2);
				}
			}
			if(Command.ReleasePreview) {
				boolean r1 = r.isContextual();
				boolean r2 = r.testContextSensitive(r.getExpression(), null);
				if(r1 != r2) {
					Verbose.FIXME("mismatch contextual: " + r.getLocalName() + " " + r1 + " " + r2);
				}
			}
			if(option.enabledInterning) {
				r.internRule();
			}
		}
		if(option.enabledExampleVerification) {
			testExample(option);
		}
	}

	
	
}
