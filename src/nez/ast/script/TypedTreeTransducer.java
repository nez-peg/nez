package nez.ast.script;

import nez.ast.Source;
import nez.ast.Symbol;
import nez.ast.TreeTransducer;

public class TypedTreeTransducer extends TreeTransducer {
	private static final Symbol Token = Symbol.tag("token");
	private static final Symbol Tree = Symbol.tag("tree");

	@Override
	public Object newNode(Symbol tag, Source s, long spos, long epos, int size, Object value) {
		return new TypedTree(tag == null ? (size == 0 ? Token : Tree) : tag, s, spos, (int) (epos - spos), size, value);
	}

	@Override
	public void link(Object node, int index, Symbol label, Object child) {
		((TypedTree) node).set(index, label, (TypedTree) child);
	}

	@Override
	public Object commit(Object node) {
		return node;
	}

	@Override
	public void abort(Object node) {
	}

}
