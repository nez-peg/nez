package nez.checker;

import nez.ast.ParsingFactory;
import nez.ast.Source;
import nez.ast.Tag;

public class ModifiableTreeFactory extends ParsingFactory {
	
	private static final Tag Token = Tag.tag("Token");
	private static final Tag Tree = Tag.tag("Tree");
	
	@Override
	public Object newNode(Tag tag, Source s, long spos, long epos, int size, Object value) {
		return new ModifiableTree(tag == null ? (size == 0 ? Token : Tree) : tag, s, spos, epos, size, value);
	}

	@Override
	public void link(Object node, int index, Object child) {
		((ModifiableTree)node).set(index, (ModifiableTree)child);
	}

	@Override
	public Object commit(Object node) {
		return (ModifiableTree)node;
	}

	@Override
	public void abort(Object node) {
	}

}