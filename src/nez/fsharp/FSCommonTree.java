package nez.fsharp;

import nez.ast.CommonTree;
import nez.ast.Source;
import nez.ast.Tag;

public class FSCommonTree extends CommonTree {
	
	

	public FSCommonTree(Tag tag, Source source, long pos, long epos, int size,
			Object value) {
		super(tag, source, pos, epos, size, value);
	}
	
	public final void setValue(Object value) {
		this.value = value;
	}
	
	public final void setTag(Tag tag) {
		this.tag = tag;
	}

}
