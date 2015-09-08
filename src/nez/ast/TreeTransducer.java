package nez.ast;

public abstract class TreeTransducer {
	public abstract Object newNode(SymbolId tag, Source s, long spos, long epos, int size, Object value);

	public abstract void link(Object node, int index, SymbolId label, Object child);

	public abstract Object commit(Object node);

	public abstract void abort(Object node);
}
