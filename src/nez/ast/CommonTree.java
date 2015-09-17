package nez.ast;

public class CommonTree extends AbstractTree<CommonTree> {

	public CommonTree(Symbol tag, Source source, long pos, int len, int size, Object value) {
		super(tag, source, pos, len, size > 0 ? new CommonTree[size] : null, value);
	}

	@Override
	protected CommonTree dupImpl() {
		return new CommonTree(this.getTag(), this.getSource(), this.getSourcePosition(), this.getLength(), this.size(), getValue());
	}

}
