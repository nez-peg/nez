package nez.ast.jcode;

import nez.ast.Source;
import nez.ast.Symbol;
import nez.ast.TreeTransducer;

public class JCodeTreeTransducer extends TreeTransducer {
	private static final Symbol Token = Symbol.tag("token");
	private static final Symbol Tree = Symbol.tag("tree");

	@Override
	public Object newNode(Symbol tag, Source s, long spos, long epos, int size, Object value) {
		return new JCodeTreeImpl(tag == null ? (size == 0 ? Token : Tree) : tag, s, spos, (int) (epos - spos), size, value);
	}

	@Override
	public void link(Object node, int index, Symbol tag, Object child) {
		((JCodeTreeImpl) node).set(index, tag, (JCodeTreeImpl) child);
	}

	@Override
	public Object commit(Object node) {
		return node;
	}

	@Override
	public void abort(Object arg0) {

	}

}
