package nez.ast.jcode;

import nez.ast.AbstractTree;
import nez.ast.Source;
import nez.ast.Symbol;

public abstract class JCodeTree extends AbstractTree<JCodeTree> {
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
	protected JCodeTreeImpl newInstance(Symbol tag, int size, Object value) {
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
}