package nez.ast;

public class CommonTree extends Tree<CommonTree> {

	public CommonTree() {
		super(Symbol.tag("prototype"), null, 0, 0, null, null);
	}

	public CommonTree(Symbol tag, Source source, long pos, int len, int size, Object value) {
		super(tag, source, pos, len, size > 0 ? new CommonTree[size] : null, value);
	}

	@Override
	public CommonTree newInstance(Symbol tag, Source source, long pos, int len, int size, Object value) {
		return new CommonTree(tag, source, pos, len, size, value);
	}

	@Override
	public void link(int n, Symbol label, Object child) {
		this.set(n, label, (CommonTree) child);
	}

	@Override
	public CommonTree newInstance(Symbol tag, int size, Object value) {
		return new CommonTree(tag, this.getSource(), this.getSourcePosition(), 0, size, value);
	}

	@Override
	protected CommonTree dupImpl() {
		return new CommonTree(this.getTag(), this.getSource(), this.getSourcePosition(), this.getLength(), this.size(), getValue());
	}

}
