package nez.ast.jcode;

import nez.ast.Source;
import nez.ast.Symbol;
import nez.ast.Tree;

public abstract class JCodeTree extends Tree<JCodeTree> {
	boolean requiredPop;

	protected JCodeTree(Symbol tag, Source source, long pos, int len, JCodeTree[] subTree, Object value) {
		super(tag, source, pos, len, subTree, value);
	}

	public abstract void requirePop();

	public abstract Class<?> getTypedClass();

	public abstract Class<?> setType(Class<?> type);

}

class JCodeTreeImpl extends JCodeTree {
	Class<?> typed = Object.class;

	public JCodeTreeImpl(Symbol tag, Source source, long pos, int len, int size, Object value) {
		super(tag, source, pos, len, size > 0 ? new JCodeTreeImpl[size] : null, value);
		this.requiredPop = false;
	}

	@Override
	public JCodeTreeImpl newInstance(Symbol tag, int size, Object value) {
		return new JCodeTreeImpl(tag, this.getSource(), this.getSourcePosition(), 0, size, value);
	}

	@Override
	public void requirePop() {
		for (JCodeTree child : this) {
			((JCodeTreeImpl) child).setRequiredPop(true);
		}
	}

	private void setRequiredPop(boolean requiredPop) {
		this.requiredPop = requiredPop;
	}

	@Override
	protected JCodeTreeImpl dupImpl() {
		return new JCodeTreeImpl(this.getTag(), this.getSource(), this.getSourcePosition(), this.getLength(), this.size(), getValue());
	}

	@Override
	public Class<?> getTypedClass() {
		return this.typed;
	}

	@Override
	public Class<?> setType(Class<?> type) {
		this.typed = type;
		return this.typed;
	}

	@Override
	protected JCodeTree newInstance(Symbol tag, Source source, long pos, int len, int objectsize, Object value) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void link(int n, Symbol label, Object child) {
		// TODO Auto-generated method stub

	}
}