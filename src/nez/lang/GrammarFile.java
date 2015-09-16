package nez.lang;

import java.util.List;

import nez.Grammar;
import nez.Strategy;
import nez.ast.CommonTree;
import nez.ast.SourcePosition;
import nez.util.UList;

public class GrammarFile extends Grammar {

	// private static HashMap<String, GrammarFile> nsMap = new HashMap<String,
	// GrammarFile>();
	//
	// public final static boolean isLoaded(String urn) {
	// return nsMap.containsKey(urn);
	// }
	//
	// public static GrammarFile newGrammarFile(NezOption option) {
	// return new GrammarFile(null, option);
	// }
	//
	public final static GrammarFile newGrammarFile(String urn, Strategy option) {
		return new GrammarFile(null, urn, option);
	}

	//
	// public final static GrammarFile loadNezFile(String urn, NezOption option)
	// throws IOException {
	// if (nsMap.containsKey(urn)) {
	// return nsMap.get(urn);
	// }
	// GrammarFile ns = null;
	// if (urn != null && !urn.endsWith(".nez")) {
	// try {
	// Class<?> c = Class.forName(urn);
	// ParserCombinator p = (ParserCombinator) c.newInstance();
	// ns = p.load();
	// } catch (ClassNotFoundException e) {
	// } catch (Exception e) {
	// Verbose.traceException(e);
	// }
	// }
	// if (ns == null) {
	// ns = new GrammarFile(urn, option);
	// NezGrammarLoader loader = new NezGrammarLoader(ns);
	// loader.load(urn);
	// }
	// nsMap.put(urn, ns);
	// return ns;
	// }

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
	final String urn;
	final Strategy option;

	GrammarFile(String ns, String urn, Strategy option) {
		super(ns);
		this.urn = urn;
		this.option = option;
	}

	public final Strategy getOption() {
		return this.option;
	}

	public final String getURN() {
		return this.urn;
	}

	public final Production addProduction(SourcePosition s, String localName, Expression e) {
		return this.addProduction(s, 0, localName, e);
	}

	public final Production addProduction(SourcePosition s, int flag, String localName, Expression e) {
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

	public Production newReducedProduction(String localName, Production p, GrammarTransducer m) {
		Production r = p.getGrammar().newProduction(localName, null);
		this.addProduction(r);
		m.updateProductionAttribute(p, r);
		r.setExpression(p.getExpression().reshape(m));
		return r;
	}

	// public final Parser newParser0(String name, NezOption option) {
	// Production r = this.getProduction(name);
	// if (r != null) {
	// return new ParserClassic(r, option);
	// }
	// Verbose.debug("unfound production: " + this.getProductionList());
	// return null;
	// }
	//
	// public final Parser newParser0(String name) {
	// return this.newParser0(name, NezOption.newDefaultOption());
	// }

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

	public List<Example> getExampleList() {
		return exampleList;
	}

	public final void addExample(Example ex) {
		if (exampleList == null) {
			exampleList = new UList<Example>(new Example[2]);
		}
		exampleList.add(ex);
	}

	public final List<String> getExampleList(String name) {
		UList<String> l = new UList<String>(new String[4]);
		if (exampleList != null) {
			for (Example ex : exampleList) {
				if (name.equals(ex.getName())) {
					l.add(ex.getText());
				}
			}
		}
		return l;
	}

	public final void checkExample() {
		if (exampleList == null) {
			for (Example ex : exampleList) {
				if (ex.isPublic) {
					Production p = this.getProduction(ex.getName());
					if (p != null) {
						p.flag |= Production.PublicProduction;
					}
				}
			}
		}
	}

}
