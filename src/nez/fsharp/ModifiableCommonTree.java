package nez.fsharp;

import nez.ast.CommonTree;
import nez.ast.Source;
import nez.ast.Tag;

public class ModifiableCommonTree extends CommonTree {

	public ModifiableCommonTree(Tag tag, Source source, long pos, int length, int size, Object value) {
		super(tag, source, pos, length, size, value);
	}
	
	public void setValue(Object value){
		this.value = value;
	}
	
	public void setTag(Tag tag){
		this.tag = tag;
	}

}
