package nez.ast;

public class CommonTreeFactory extends ParsingFactory {
	private static final Tag Token = Tag.tag("Token");
	private static final Tag Tree = Tag.tag("Tree");
	
	@Override
	public Object newNode(Tag tag, Source s, long spos, long epos, int size, Object value) {
		return new CommonTree(tag == null ? (size == 0 ? Token : Tree) : tag, s, spos, epos, size, value);
	}

	@Override
	public void link(Object node, int index, Object child) {
		((CommonTree)node).set(index, (CommonTree)child);
	}

	@Override
	public Object commit(Object node) {
		return node;
	}

	@Override
	public void abort(Object node) {
	}


}
