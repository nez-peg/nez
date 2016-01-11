package nez.ast;

import java.util.HashMap;

import nez.util.UList;

public class Symbol {
	private static HashMap<String, Symbol> tagIdMap = new HashMap<>();
	private static UList<Symbol> tagNameList = new UList<>(new Symbol[64]);

	public final static Symbol tag(String tagName) {
		Symbol tag = tagIdMap.get(tagName);
		if (tag == null) {
			tag = new Symbol(tagIdMap.size(), tagName);
			tagIdMap.put(tagName, tag);
			tagNameList.add(tag);
		}
		return tag;
	}

	public final static int id(String tagName) {
		return tag(tagName).id;
	}

	public final static Symbol tag(int tagId) {
		return tagNameList.ArrayValues[tagId];
	}

	public final static Symbol NullSymbol = tag("");
	public final static Symbol MetaSymbol = tag("$");

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
