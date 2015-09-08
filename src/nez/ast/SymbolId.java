package nez.ast;

import java.util.HashMap;

import nez.util.UList;

public class SymbolId {
	private static HashMap<String, SymbolId> tagIdMap = new HashMap<String, SymbolId>();
	private static UList<SymbolId> tagNameList = new UList<SymbolId>(new SymbolId[64]);

	public final static SymbolId tag(String tagName) {
		SymbolId tag = tagIdMap.get(tagName);
		if (tag == null) {
			tag = new SymbolId(tagIdMap.size(), tagName);
			tagIdMap.put(tagName, tag);
			tagNameList.add(tag);
		}
		return tag;
	}

	public final static int id(String tagName) {
		return tag(tagName).id;
	}

	public final static SymbolId tag(int tagId) {
		return tagNameList.ArrayValues[tagId];
	}

	public final static SymbolId NullTag = tag("");
	public final static SymbolId MetaTag = tag("keyvalue");

	final int id;
	final String symbol;

	private SymbolId(int id, String symbol) {
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
