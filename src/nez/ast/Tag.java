package nez.ast;

import java.util.HashMap;

import nez.util.UList;

public class Tag {
	private static HashMap<String, Tag> idMap = new HashMap<String, Tag>();
	private static UList<Tag> tagNameList = new UList<Tag>(new Tag[64]);
	
	public final static Tag tag(String tagName) {
		Tag tag = idMap.get(tagName);
		if(tag == null) {
			tag = new Tag(idMap.size(), tagName);
			idMap.put(tagName, tag);
			tagNameList.add(tag);
		}
		return tag;
	}
	
	public final static int id(String tagName) {
		return tag(tagName).id;
	}
	
	public final static Tag tag(int tagId) {
		return tagNameList.ArrayValues[tagId];
	}
	
	public final int    id;
	public final String name;
	
	private Tag(int tagid, String name) {
		this.id = tagid;
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
