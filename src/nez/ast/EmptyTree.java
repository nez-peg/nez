package nez.ast;

class EmptyTree extends Tree<EmptyTree> {

	public EmptyTree() {
		super(null, null, 0, 0, null, null);
	}

	@Override
	public EmptyTree newInstance(Symbol tag, Source source, long pos, int len, int size, Object value) {
		return null;
	}

	@Override
	public void link(int n, Symbol label, Object child) {
	}

	@Override
	public EmptyTree newInstance(Symbol tag, int size, Object value) {
		return null;
	}

	@Override
	protected EmptyTree dupImpl() {
		return null;
	}

}
