package nez.ast;

public abstract class ParsingFactory {
	public abstract Object newNode(Tag tag, Source s, long spos, long epos, int size, Object value);
	public abstract void link(Object node, int index, Object child);
	public abstract Object commit(Object node);
	public abstract void abort(Object node);
}
