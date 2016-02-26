package nez.debugger;

import nez.ast.CommonTree;
import nez.ast.Source;
import nez.ast.Symbol;

public class CommonTreeTransducer extends TreeTransducer {

	@Override
	public Object newNode(Symbol tag, Source s, long spos, long epos, int size, Object value) {
		return new CommonTree(tag == null ? Symbol.Null : tag, s, spos, (int) (epos - spos), size, value);
	}

	@Override
	public void link(Object node, int index, Symbol label, Object child) {
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
