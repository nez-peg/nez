package nez.lang;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import nez.NezOption;
import nez.ParserCombinator;
import nez.ast.CommonTree;
import nez.ast.SourcePosition;
import nez.main.Command;
import nez.main.Verbose;
import nez.peg.celery.Celery;
import nez.peg.dtd.DTDConverter;
import nez.util.ConsoleUtils;
import nez.util.UList;

public class GrammarFile extends GrammarMap {

	private static int nsid = 0;
	private static HashMap<String, GrammarFile> nsMap = new HashMap<String, GrammarFile>();

	public final static boolean isLoaded(String urn) {
		return nsMap.containsKey(urn);
	}

	public static GrammarFile newGrammarFile(NezOption option) {
		return new GrammarFile(nsid++, null, option);
	}

	public final static GrammarFile newGrammarFile(String urn, NezOption option) {
		if (urn != null && nsMap.containsKey(urn)) {
			return nsMap.get(urn);
		}
		GrammarFile ns = new GrammarFile(nsid++, urn, option);
		if (urn != null) {
			nsMap.put(urn, ns);
		}
		return ns;
	}

	public final static GrammarFile loadNezFile(String urn, NezOption option) throws IOException {
		if (nsMap.containsKey(urn)) {
			return nsMap.get(urn);
		}
		GrammarFile ns = null;
		if (urn != null && !urn.endsWith(".nez")) {
			try {
				Class<?> c = Class.forName(urn);
				ParserCombinator p = (ParserCombinator) c.newInstance();
				ns = p.load();
			} catch (ClassNotFoundException e) {
			} catch (Exception e) {
				Verbose.traceException(e);
			}
		}
		if (ns == null) {
			ns = new GrammarFile(nsid++, urn, option);
			NezGrammarLoader loader = new NezGrammarLoader(ns);
			loader.load(urn);
		}
		nsMap.put(urn, ns);
		return ns;
	}

	public final static GrammarFile loadGrammarFile(String urn, NezOption option) throws IOException {
		if (urn.endsWith(".dtd")) {
			return DTDConverter.loadGrammar(urn, option);
		}
		if (urn.endsWith(".celery")) {
			return Celery.loadGrammar(urn, option);
		}
		return loadNezFile(urn, option);
	}

	public final static String nameUniqueName(String ns, String name) {
		return ns + ":" + name;
	}

	public final static String nameNamespaceName(String ns, String name) {
		return ns == null ? name : ns + "." + name;
	}

	public final static String nameTerminalProduction(String t) {
		return "\"" + t + "\"";
	}

	// fields

	// final int id;
	final String urn;
	// final String ns;
	// final UMap<Production> ruleMap;
	// final UList<String> nameList;
	final NezOption option;

	private GrammarFile(int id, String urn, NezOption option) {
		super(null);
		this.urn = urn;
		this.option = option;
		// this.id = id;
		// String ns = "g";
		// if (urn != null) {
		// int loc = urn.lastIndexOf('/');
		// if (loc != -1) {
		// ns = urn.substring(loc + 1);
		// }
		// ns = ns.replace(".nez", "");
		// }
		// this.ns = ns;
		// this.ruleMap = new UMap<Production>();
		// this.nameList = new UList<String>(new String[8]);
	}

	public final NezOption getOption() {
		return this.option;
	}

	public final String getURN() {
		return this.urn;
	}

	public final Production defineProduction(SourcePosition s, String localName, Expression e) {
		return this.defineProduction(s, 0, localName, e);
	}

	public final Production defineProduction(SourcePosition s, int flag, String localName, Expression e) {
		Production p = new Production(s, flag, this, localName, e);
		addProduction(p);
		return p;
	}

	public final Production importProduction(String ns, Production p) {
		// this.ruleMap.put(nameNamespaceName(ns, p.getLocalName()), p);
		// addProduction(p);
		// return p;
		throw new RuntimeException("FIXME");
	}

	public final List<String> getNonterminalList() {
		ArrayList<String> l = new ArrayList<String>();
		for (Production p : this.getProductionList()) {
			String s = p.getLocalName();
			char c = s.charAt(0);
			if (!Character.isUpperCase(c)) {
				continue;
			}
			l.add(s);
		}
		Collections.sort(l);
		return l;
	}

	public Production newReducedProduction(String localName, Production p, GrammarReshaper m) {
		Production r = p.newProduction(localName);
		this.addProduction(r);
		m.updateProductionAttribute(p, r);
		r.setExpression(p.getExpression().reshape(m));
		return r;
	}

	public final Production newProduction(int flag, String name, Expression e) {
		Production r = new Production(null, flag, this, name, e);
		this.addProduction(r);
		return r;
	}

	public final Grammar newParser(String name, NezOption option) {
		Production r = this.getProduction(name);
		if (r != null) {
			return new Grammar(r, option);
		}
		Verbose.debug("newParser" + this.getProductionList());
		return null;
	}

	public final Grammar newGrammar(String name) {
		return this.newParser(name, NezOption.newDefaultOption());
	}

	public void dump() {
		for (Production r : this.getProductionList()) {
			ConsoleUtils.println(r);
		}
	}

	private FormatterMap fmtMap;

	public final void addFormatter(String tag, int size, Formatter fmt) {
		if (fmtMap == null) {
			fmtMap = new FormatterMap();
		}
		fmtMap.set(tag, size, fmt);
	}

	public final Formatter getFormatter(String tag, int size) {
		if (fmtMap != null) {
			return fmtMap.get(tag, size);
		}
		return null;
	}

	public final String formatCommonTree(CommonTree node) {
		return Formatter.format(this, node);
	}

	private UList<Example> exampleList;

	final void addExample(Example ex) {
		if (exampleList == null) {
			exampleList = new UList<Example>(new Example[2]);
		}
		exampleList.add(ex);
	}

	final void testExample(NezOption option) {
		if (exampleList != null) {
			long t1 = System.nanoTime();
			for (Example ex : exampleList) {
				ex.test(this, option);
			}
			long t2 = System.nanoTime();
			if (Verbose.Example) {
				Verbose.println("Elapsed time (Example Tests): " + ((t2 - t1) / 1000000) + "ms");
			}
		}
	}

	public void verify() {
		NameAnalysis nameAnalyzer = new NameAnalysis();
		nameAnalyzer.analyze(this.getProductionList()/* getDefinedRuleList() */);
		// if(this.foundError) {
		// ConsoleUtils.exit(1, "FatalGrammarError");
		// }
		// type check
		for (Production p : this.getProductionList()) {
			if (p.isTerminal()) {
				continue;
			}
			p.reshape(new Typestate(this));
		}
		GrammarOptimizer optimizer = null;
		if (!option.enabledAsIsGrammar) {
			optimizer = new GrammarOptimizer(this.option);
		}
		for (Production r : this.getProductionList()) {
			if (r.isTerminal()) {
				continue;
			}
			if (Verbose.Grammar) {
				r.dump();
			}
			if (Command.ReleasePreview) {
				boolean r1 = r.isConditional();
				boolean r2 = r.testCondition(r.getExpression(), null);
				if (r1 != r2) {
					Verbose.FIXME("mismatch condition: " + r.getLocalName() + " " + r1 + " " + r2);
				}
			}
			if (Command.ReleasePreview) {
				boolean r1 = r.isContextual();
				boolean r2 = r.testContextSensitive(r.getExpression(), null);
				if (r1 != r2) {
					Verbose.FIXME("mismatch contextual: " + r.getLocalName() + " " + r1 + " " + r2);
				}
			}
			if (optimizer != null) {
				optimizer.optimize(r);
			}
			if (option.enabledInterning) {
				r.internRule();
			}
		}
		if (option.enabledExampleVerification) {
			testExample(option);
		}
	}

}
