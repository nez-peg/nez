package nez.ast;

public class CommonTreeTransducer extends TreeTransducer {
	private static final SymbolId Token = SymbolId.tag("token");
	private static final SymbolId Tree = SymbolId.tag("tree");

	@Override
	public Object newNode(SymbolId tag, Source s, long spos, long epos, int size, Object value) {
		return new CommonTree(tag == null ? (size == 0 ? Token : Tree) : tag, s, spos, (int) (epos - spos), size, value);
	}

	@Override
	public void link(Object node, int index, SymbolId label, Object child) {
		((CommonTree) node).set(index, label, (CommonTree) child);
	}

	@Override
	public Object commit(Object node) {
		return node;
	}

	@Override
	public void abort(Object node) {
	}

}
