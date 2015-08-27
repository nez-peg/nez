package nez.ast;

import java.util.HashMap;

import nez.util.UList;

public class Tag {
	private static HashMap<String, Tag> tagIdMap = new HashMap<String, Tag>();
	private static UList<Tag> tagNameList = new UList<Tag>(new Tag[64]);
	
	public final static Tag tag(String tagName) {
		Tag tag = tagIdMap.get(tagName);
		if(tag == null) {
			tag = new Tag(tagIdMap.size(), tagName);
			tagIdMap.put(tagName, tag);
			tagNameList.add(tag);
		}
		return tag;
	}
	
	public final static int id(String tagName) {
		return tag(tagName).tagId;
	}
	
	public final static Tag tag(int tagId) {
		return tagNameList.ArrayValues[tagId];
	}	
	
	public final static Tag NullTag = tag("");
	public final static Tag MetaTag = tag("keyvalue");

	public final int    tagId;
	public final String name;
	
	private Tag(int tagid, String name) {
		this.tagId = tagid;
		this.name = name;
	}
	
	public final String getName() {
		return name;
	}
	
	@Override
	public String toString() {
		return this.name;
	}
}
