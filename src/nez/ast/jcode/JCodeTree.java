package nez.ast.jcode;

import nez.ast.AbstractTree;
import nez.ast.Source;
import nez.ast.Tag;

public abstract class JCodeTree extends AbstractTree<JCodeTree> {

	protected JCodeTree(Tag tag, Source source, long pos, int len, JCodeTree[] subTree, Object value) {
		super(tag, source, pos, len, subTree, value);
	}

	public abstract Class<?> getTypedClass();

}

class JCodeTreeImpl extends JCodeTree {
	Class<?> typed = Object.class;

	public JCodeTreeImpl(Tag tag, Source source, long pos, int len, int size, Object value) {
		super(tag, source, pos, len, size > 0 ? new JCodeTreeImpl[size] : null, value);
	}

	@Override
	protected JCodeTreeImpl dupImpl() {
		return new JCodeTreeImpl(this.getTag(), this.getSource(), this.getSourcePosition(), this.getLength(),
				this.size(), getValue());
	}

	@Override
	public Class<?> getTypedClass() {
		return this.typed;
	}
}