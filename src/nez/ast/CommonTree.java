package nez.ast;

import java.util.AbstractList;

import nez.SourceContext;
import nez.util.StringUtils;

public class CommonTree extends AbstractList<CommonTree> implements SourcePosition {
//	public static long gcCount = 0;
//	protected void finalize() {
//		//System.out.print(".");
//		gcCount++;
//	}
	private Source    source;
	private Tag       tag;
	private long      pos;
	private int       length;
	private Object    value;
	CommonTree             parent = null;
	private CommonTree     subTree[] = null;

	public CommonTree(Tag tag, Source source, long pos, long epos, int size, Object value) {
		this.tag        = tag;
		this.source     = source;
		this.pos        = pos;
		this.length     = (int)(epos - pos);
		if(size > 0) {
			this.subTree = new CommonTree[size];
		}
		this.value = value;
	}
	
	CommonTree(Tag tag, Source source, long pos, int length, int size, Object value) {
		this.tag        = tag;
		this.source     = source;
		this.pos        = pos;
		this.length     = length;
		this.subTree = new CommonTree[size];
		this.value = value;
	}

	CommonTree(Tag tag, Source source, long pos, int length, Object value) {
		this.tag        = tag;
		this.source     = source;
		this.pos        = pos;
		this.length     = length;
		this.value = value;
	}

	public CommonTree dup() {
		if(this.subTree != null) {
			CommonTree t = new CommonTree(this.tag, this.source, pos, this.length, this.subTree.length, value);
			for(int i = 0; i < subTree.length; i++) {
				if(this.subTree[i]!=null) {
					t.subTree[i] = this.subTree[i].dup();
				}
			}
			return t;
		}
		else {
			return new CommonTree(this.tag, this.source, pos, this.length, value);
		}
	}

	public int count() {
		int c = 1;
		for(CommonTree t: this) {
			if(t != null) {
				c += t.count();
			}
		}
		return c;
	}
	
	void link(int index, CommonTree child) {
		this.set(index, child);
	}

	public final Tag getTag() {
		return this.tag;
	}

	public final boolean is(Tag t) {
		return this.tag == t;
	}

	public final CommonTree getParent() {
		return this.parent;
	}

	public final long getSourcePosition() {
		return this.pos;
	}

	public final int getLength() {
		return this.length;
	}

	@Override
	public final boolean isEmpty() {
		return this.length == 0;
	}

	public final Source getSource() {
		return this.source;
	}

	@Override
	public final String formatSourceMessage(String type, String msg) {
		return this.source.formatPositionLine(type, this.getSourcePosition(), msg);
	}
	
	// subTree[]
	
	@Override
	public final int size() {
		if(this.subTree == null) {
			return 0;
		}
		return this.subTree.length;
	}

	@Override
	public final CommonTree get(int index) {
		return this.subTree[index];
	}

	public final CommonTree get(int index, CommonTree defaultValue) {
		if(index < this.size()) {
			return this.subTree[index];
		}
		return defaultValue;
	}

	@Override
	public final CommonTree set(int index, CommonTree node) {
		CommonTree oldValue = null;
//		if(!(index < this.size())){
//			this.expandAstToSize(index+1);
//		}
		oldValue = this.subTree[index];
		this.subTree[index] = node;
		node.parent = this;
		return oldValue;
	}
	
//	private final void expandAstToSize(int newSize) {
//		if(newSize > this.size()) {
//			this.resizeAst(newSize);
//		}
//	}
//
//	
//	public final void append(AST childNode) {
//		int size = this.size();
//		this.expandAstToSize(size+1);
//		this.subTree[size] = childNode;
//		childNode.parent = this;
//	}
//	
//	private void resizeAst(int size) {
//		if(this.subTree == null && size > 0) {
//			this.subTree = new CommonTree[size];
//		}
//		else if(size == 0){
//			this.subTree = null;
//		}
//		else if(this.subTree.length != size) {
//			CommonTree[] newast = new CommonTree[size];
//			if(size > this.subTree.length) {
//				System.arraycopy(this.subTree, 0, newast, 0, this.subTree.length);
//			}
//			else {
//				System.arraycopy(this.subTree, 0, newast, 0, size);
//			}
//			this.subTree = newast;
//		}
//	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		this.stringfy("", sb);
		return sb.toString();
	}

	final void stringfy(String indent, StringBuilder sb) {
		sb.append("\n");
		sb.append(indent);
		sb.append("#");
		sb.append(this.getTag().getName());
		sb.append("[");
		if(this.subTree == null) {
			sb.append(" ");
			StringUtils.formatQuoteString(sb, '\'', this.getText(), '\'');
			sb.append("]");
		}
		else {
			String nindent = "   " + indent;
			for(int i = 0; i < this.size(); i++) {
				if(this.subTree[i] == null) {
					sb.append("\n");
					sb.append(nindent);
					sb.append("null");
				}
				else {
					this.subTree[i].stringfy(nindent, sb);
				}
			}
			sb.append("\n");
			sb.append(indent);
			sb.append("]");
		}
	}
	
	// 
	
	public final String getText() {
		if(this.value != null) {
			return this.value.toString();
		}
		if(this.source != null) {
			this.value = this.source.substring(this.getSourcePosition(), this.getSourcePosition() + this.getLength());
			return this.value.toString();
		}
		return "";
	}
	
	public final String textAt(int index, String defaultValue) {
		if(index < this.size()) {
			return this.get(index).getText();
		}
		return defaultValue;
	}
	
	public final boolean containsToken(String token) {
		for(CommonTree sub : this) {
			if(sub.containsToken(token)) {
				return true;
			}
		}
		return token.equals(getText());
	}

	/**
	 * Create new input stream 
	 * @return SourceContext
	 */
	
	public final SourceContext newSourceContext() {
		return SourceContext.newStringSourceContext(this.source.getResourceName(), 
				this.source.linenum(this.getSourcePosition()), this.getText());
	}

//
//	public final AST findParentNode(int tagName) {
//		AST node = this;
//		while(node != null) {
//			if(node.is(tagName)) {
//				return node;
//			}
//			node = node.parent;
//		}
//		return null;
//	}
//
//	public final AST getPath(String path) {
//		int loc = path.indexOf('#', 1);
//		if(loc == -1) {
//			return this.getPathByTag(path);
//		}
//		else {
//			String[] paths = path.split("#");
//			Main._Exit(1, "TODO: path = " + paths.length + ", " + paths[0]);
//			return null;
//		}
//	}
//	
//	private final AST getPathByTag(String tagName) {
//		int tagId = Tag.tagId(tagName);
//		for(int i = 0; i < this.size(); i++) {
//			AST p = this.get(i);
//			if(p.is(tagId)) {
//				return p;
//			}
//		}
//		return null;
//	}
	
}

