package nez.ast;

import java.util.HashMap;

import nez.util.UList;

public class Symbol {
	private static HashMap<String, Symbol> tagIdMap = new HashMap<String, Symbol>();
	private static UList<Symbol> tagNameList = new UList<Symbol>(new Symbol[64]);

	public final static Symbol unique(String s) {
		Symbol tag = tagIdMap.get(s);
		if (tag == null) {
			tag = new Symbol(tagIdMap.size(), s);
			tagIdMap.put(s, tag);
			tagNameList.add(tag);
		}
		return tag;
	}

	public final static int uniqueId(String symbol) {
		return unique(symbol).id;
	}

	public final static Symbol tag(int tagId) {
		return tagNameList.ArrayValues[tagId];
	}

	public final static Symbol Null = unique("");
	public final static Symbol MetaSymbol = unique("$");
	// public final static Symbol tokenTag = unique(""); // for compatibility
	// Cnez
	// public final static Symbol treeTag = unique("");

	final int id;
	final String symbol;

	private Symbol(int id, String symbol) {
		this.id = id;
		this.symbol = symbol;
	}

	public final int id() {
		return this.id;
	}

	public final String getSymbol() {
		return symbol;
	}

	@Override
	public String toString() {
		return this.symbol;
	}
}
