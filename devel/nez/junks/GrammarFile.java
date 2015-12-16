package nez.junks;

import nez.lang.Grammar;


public class GrammarFile extends Grammar {
	//
	// public final static GrammarFile newGrammarFile(String urn, ParserStrategy
	// option) {
	// return new GrammarFile(null, urn, option);
	// }
	//
	// public final static String nameUniqueName(String ns, String name) {
	// return ns + ":" + name;
	// }
	//
	// public final static String nameNamespaceName(String ns, String name) {
	// return ns == null ? name : ns + "." + name;
	// }
	//
	//
	// // fields
	// final String urn;
	// final ParserStrategy strategy;
	// String desc;
	//
	// GrammarFile(String ns, String urn, ParserStrategy strategy) {
	// super(ns);
	// this.urn = urn;
	// this.strategy = strategy;
	// this.desc = "";
	// }
	//
	// public final ParserStrategy getStrategy() {
	// return this.strategy;
	// }
	//
	// public final String getURN() {
	// return this.urn;
	// }
	//
	// public final void setDesc(String desc) {
	// this.desc = desc;
	// }
	//
	// public final String getDesc() {
	// return this.desc;
	// }
	//
	// public final Production addProduction(SourcePosition s, String localName,
	// Expression e) {
	// return this.addProduction(s, 0, localName, e);
	// }
	//
	// public final Production addProduction(SourcePosition s, int flag, String
	// localName, Expression e) {
	// Production p = new Production(s, flag, this, localName, e);
	// addProduction(p);
	// return p;
	// }
	//
	// public final Production importProduction(String ns, Production p) {
	// // this.ruleMap.put(nameNamespaceName(ns, p.getLocalName()), p);
	// // addProduction(p);
	// // return p;
	// throw new RuntimeException("FIXME");
	// }
	//
	// private FormatterMap fmtMap;
	//
	// public final void addFormatter(String tag, int size, Formatter fmt) {
	// if (fmtMap == null) {
	// fmtMap = new FormatterMap();
	// }
	// fmtMap.set(tag, size, fmt);
	// }
	//
	// public final Formatter getFormatter(String tag, int size) {
	// if (fmtMap != null) {
	// return fmtMap.get(tag, size);
	// }
	// return null;
	// }
	//
	// public final String formatCommonTree(CommonTree node) {
	// return Formatter.format(this, node);
	// }
	//
}
