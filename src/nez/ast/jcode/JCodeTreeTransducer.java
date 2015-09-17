package nez.ast.jcode;

import nez.ast.Source;
import nez.ast.Symbol;
import nez.ast.TreeTransducer;

public class JCodeTreeTransducer extends TreeTransducer {

	static final Symbol Expression = Symbol.tag("node");

	@Override
	public Object newNode(Symbol tag, Source s, long spos, long epos, int size, Object value) {
		return new JCodeTreeImpl(tag == null ? Expression : tag, s, spos, (int) (epos - spos), size, value);
	}

	@Override
	public void link(Object node, int index, Symbol tag, Object child) {
		((JCodeTreeImpl) node).set(index, (JCodeTreeImpl) child);
	}

	@Override
	public Object commit(Object node) {
		return node;
	}

	@Override
	public void abort(Object arg0) {
	}

}
