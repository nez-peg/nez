package nez.fsharp;

import java.util.AbstractList;
import java.util.ArrayList;

import nez.SourceContext;
import nez.ast.Source;
import nez.ast.SourcePosition;
import nez.ast.Tag;
import nez.util.StringUtils;

public class ModifiableTree extends AbstractList<ModifiableTree> implements SourcePosition {

	private Source source;
	private Tag tag;
	private long pos;
	private int length;
	private Object value;
	ModifiableTree parent = null;
	private ArrayList<ModifiableTree> subTree = null;
	public FSharpScope fsClass;
	public FSharpScope scope;

	public ModifiableTree(Tag tag, Source source, long pos, long epos, int size, Object value) {
		this.tag = tag;
		this.source = source;
		this.pos = pos;
		this.length = (int) (epos - pos);
		if(size > 0) {
			this.subTree = new ArrayList<ModifiableTree>();
			for(int i = 0; i < size; i++) {
				subTree.add(null);
			}
		}
		this.value = value;
	}

	ModifiableTree(Tag tag, Source source, long pos, int length, int size, Object value) {
		this.tag = tag;
		this.source = source;
		this.pos = pos;
		this.length = length;
		this.subTree = new ArrayList<ModifiableTree>();
		this.value = value;
	}

	ModifiableTree(Tag tag, Source source, long pos, int length, Object value) {
		this.tag = tag;
		this.source = source;
		this.pos = pos;
		this.length = length;
		this.value = value;
	}

	public ModifiableTree dup() {
		if(this.subTree != null) {
			ModifiableTree t = new ModifiableTree(this.tag, this.source, pos, this.length,
					this.subTree.size(), value);
			for(int i = 0; i < subTree.size(); i++) {
				if(this.subTree.get(i) != null) {
					t.subTree.set(i, this.subTree.get(i).dup());
				}
			}
			return t;
		}
		else {
			return new ModifiableTree(this.tag, this.source, pos, this.length, value);
		}
	}

	public int count() {
		int c = 1;
		for(ModifiableTree t : this) {
			if(t != null) {
				c += t.count();
			}
		}
		return c;
	}

	void link(int index, ModifiableTree child) {
		this.set(index, child);
	}

	public final Tag getTag() {
		return this.tag;
	}

	public final boolean is(Tag t) {
		return this.tag == t;
	}

	public final ModifiableTree getParent() {
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
		return this.subTree.size();
	}

	@Override
	public final ModifiableTree get(int index) {
		return this.subTree.get(index);
	}

	public final ModifiableTree get(int index, ModifiableTree defaultValue) {
		if(index < this.size()) {
			return this.subTree.get(index);
		}
		return defaultValue;
	}

	@Override
	public final ModifiableTree set(int index, ModifiableTree node) {
		node.parent = this;
		return this.subTree.set(index, node);
	}

	public final void insert(int index, ModifiableTree node) {
		node.parent = this;
		this.subTree.add(index, node);
	}

	public final boolean add(ModifiableTree node) {
		node.parent = this;
		return this.subTree.add(node);
	}

	public final ModifiableTree remove(int index) {
		return this.subTree.remove(index);
	}

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
				if(this.subTree.get(i) == null) {
					sb.append("\n");
					sb.append(nindent);
					sb.append("null");
				}
				else {
					this.subTree.get(i).stringfy(nindent, sb);
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
			this.value = this.source.substring(this.getSourcePosition(), this.getSourcePosition()
					+ this.getLength());
			return this.value.toString();
		}
		return "";
	}

	public final Object getValue() {
		return this.value;
	}

	public final int getIndexInParentNode() {
		int index = -1;
		if(this.parent != null) {
			for(int i = 0; i < this.parent.size(); i++) {
				if(this.parent.get(i) == this) {
					index = i;
				}
			}
		}
		return index;
	}

	public final String textAt(int index, String defaultValue) {
		if(index < this.size()) {
			return this.get(index).getText();
		}
		return defaultValue;
	}

	public final boolean containsToken(String token) {
		for(ModifiableTree sub : this) {
			if(sub.containsToken(token)) {
				return true;
			}
		}
		return token.equals(getText());
	}

	/**
	 * Create new input stream
	 * 
	 * @return SourceContext
	 */

	public final SourceContext newSourceContext() {
		return SourceContext.newStringSourceContext(this.source.getResourceName(),
				this.source.linenum(this.getSourcePosition()), this.getText());
	}

	public void setValue(Object value) {
		this.value = value;
	}

	public void setTag(Tag tag) {
		this.tag = tag;
	}

	@Override
	public String formatDebugSourceMessage(String msg) {
		return null;
	}

}
