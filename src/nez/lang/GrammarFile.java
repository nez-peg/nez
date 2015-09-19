package nez.lang;

import java.util.List;

import nez.Grammar;
import nez.Strategy;
import nez.ast.CommonTree;
import nez.ast.SourcePosition;
import nez.util.UList;

public class GrammarFile extends Grammar {

	public final static GrammarFile newGrammarFile(String urn, Strategy option) {
		return new GrammarFile(null, urn, option);
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
	final String urn;
	final Strategy strategy;
	String desc;

	GrammarFile(String ns, String urn, Strategy strategy) {
		super(ns);
		this.urn = urn;
		this.strategy = strategy;
		this.desc = "";
	}

	public final Strategy getStrategy() {
		return this.strategy;
	}

	public final String getURN() {
		return this.urn;
	}

	public final void setDesc(String desc) {
		this.desc = desc;
	}

	public final String getDesc() {
		return this.desc;
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
