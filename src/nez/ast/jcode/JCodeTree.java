package nez.ast.jcode;

import nez.ast.AbstractTree;
import nez.ast.Source;
import nez.ast.Tag;

public class JCodeTree extends AbstractTree<JCodeTree> {
	Class<?> typed;
	public JCodeTree(Tag tag, Source source, long pos, int len, int size, Object value) {
		super(tag, source, pos, len, size > 0 ? new JCodeTree[size] : null, value);
	}
	@Override
	protected JCodeTree dupImpl() {
		return new JCodeTree(this.getTag(), 
			this.getSource(), this.getSourcePosition(), this.getLength(), this.size(), getValue());
	}

}
