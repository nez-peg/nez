package nez.ast;

import java.util.AbstractList;

import nez.io.SourceContext;
import nez.util.StringUtils;

public abstract class Tree<E extends Tree<E>> extends AbstractList<E> implements SourcePosition {
	private final static Symbol[] EmptyLabels = new Symbol[0];

	protected Symbol tag;
	protected Source source;
	protected int pos;
	protected int length;
	protected Object value;
	protected Symbol[] labels;
	protected E[] subTree;

	protected Tree(Symbol tag, Source source, long pos, int len, E[] subTree, Object value) {
		this.tag = tag;
		this.source = source;
		this.pos = (int) pos;
		this.length = len;
		this.subTree = subTree;
		this.value = value;
		this.labels = (this.subTree != null) ? new Symbol[this.subTree.length] : EmptyLabels;
	}

	protected abstract E newInstance(Symbol tag, int len, Object value);

	protected abstract E dupImpl();

	public final E dup() {
		E t = dupImpl();
		if (this.subTree != null) {
			for (int i = 0; i < subTree.length; i++) {
				if (this.subTree[i] != null) {
					t.subTree[i] = this.subTree[i].dup();
					t.labels[i] = this.labels[i];
				}
			}
		}
		return t;
	}

	/* Source */

	public final Source getSource() {
		return this.source;
	}

	public final long getSourcePosition() {
		return this.pos;
	}

	public final int getLineNum() {
		return (int) this.source.linenum(this.pos);
	}

	public final int getColumn() {
		return this.source.column(this.pos);
	}

	public final int getLength() {
		return this.length;
	}

	/* Tag, Type */

	public final Symbol getTag() {
		return this.tag;
	}

	public final boolean is(Symbol t) {
		return t == this.getTag();
	}

	public <T> T getType() {
		return null;
	}

	@Override
	public int size() {
		return this.labels.length;
	}

	@Override
	public final boolean isEmpty() {
		return this.size() == 0;
	}

	public final Symbol getLabel(int index) {
		return this.labels[index];
	}

	public final boolean isAllLabeled() {
		for (int i = 0; i < this.labels.length; i++) {
			if (labels[i] == null) {
				return false;
			}
		}
		return true;
	}

	public final int countSubNodes() {
		int c = 1;
		for (E t : this) {
			if (t != null) {
				c += t.countSubNodes();
			}
		}
		return c;
	}

	@Override
	public E get(int index) {
		return this.subTree[index];
	}

	public final E get(int index, E defaultValue) {
		if (index < this.size()) {
			return this.subTree[index];
		}
		return defaultValue;
	}

	@Override
	public final E set(int index, E node) {
		E oldValue = null;
		oldValue = this.subTree[index];
		this.subTree[index] = node;
		// node.setParent(this);
		return oldValue;
	}

	public final void set(int index, Symbol label, E node) {
		this.labels[index] = label;
		this.subTree[index] = node;
	}

	public final int indexOf(Symbol label) {
		for (int i = 0; i < labels.length; i++) {
			if (labels[i] == label) {
				return i;
			}
		}
		return -1;
	}

	public final E get(Symbol label) {
		for (int i = 0; i < labels.length; i++) {
			if (labels[i] == label) {
				return this.subTree[i];
			}
		}
		throw new RuntimeException("undefined label: " + label);
	}

	public final E get(Symbol label, E defval) {
		for (int i = 0; i < labels.length; i++) {
			if (labels[i] == label) {
				return this.subTree[i];
			}
		}
		return defval;
	}

	public final void set(Symbol label, E defval) {
		for (int i = 0; i < labels.length; i++) {
			if (labels[i] == label) {
				this.subTree[i] = defval;
				return;
			}
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		this.stringfy("", null, sb);
		return sb.toString();
	}

	public void stringfy(String indent, Symbol label, StringBuilder sb) {
		if (indent.length() > 0) {
			sb.append("\n");
		}
		sb.append(indent);
		if (label != null) {
			sb.append("$");
			sb.append(label);
			sb.append(" ");
		}
		sb.append("#");
		sb.append(this.getTag().getSymbol());
		sb.append("[");
		if (this.subTree == null) {
			sb.append(" ");
			StringUtils.formatQuoteString(sb, '\'', this.toText(), '\'');
			sb.append("]");
		} else {
			String nindent = "   " + indent;
			for (int i = 0; i < this.size(); i++) {
				if (this.subTree[i] == null) {
					sb.append("\n");
					sb.append(nindent);
					sb.append("null");
				} else {
					this.subTree[i].stringfy(nindent, this.labels[i], sb);
				}
			}
			sb.append("\n");
			sb.append(indent);
			sb.append("]");
		}
	}

	protected final Object getValue() {
		return this.value;
	}

	public final String toText() {
		if (this.value != null) {
			return this.value.toString();
		}
		if (this.source != null) {
			long pos = this.getSourcePosition();
			long epos = pos + this.length;
			this.value = this.source.substring(pos, epos);
			// try {
			// this.value = this.source.substring(pos, epos);
			// } catch (Exception e) {
			// System.out.println(e);
			// System.out.println("pos: " + pos + " epos: " + epos + " < " +
			// this.getSource().length());
			// this.value = "";
			// }
			return this.value.toString();
		}
		return "";
	}

	public final String getText(int index, String defval) {
		if (index < this.size()) {
			return this.get(index).toText();
		}
		return defval;
	}

	public final String getText(Symbol label, String defval) {
		for (int i = 0; i < this.labels.length; i++) {
			if (labels[i] == label) {
				return getText(i, defval);
			}
		}
		return defval;
	}

	public final int toInt(int defvalue) {
		if (this.value instanceof Number) {
			return ((Number) this.value).intValue();
		}
		try {
			String s = this.toText();
			int num = Integer.parseInt(s);
			if (this.value == null) {
				this.value = new Integer(num);
			}
			return num;
		} catch (NumberFormatException e) {
		}
		return defvalue;
	}

	public final int getInt(int index, int defvalue) {
		if (index < this.size()) {
			return this.get(index).toInt(defvalue);
		}
		return defvalue;
	}

	public final int getInt(Symbol label, int defvalue) {
		for (int i = 0; i < this.labels.length; i++) {
			if (labels[i] == label) {
				return getInt(i, defvalue);
			}
		}
		return defvalue;
	}

	@Override
	public final String formatSourceMessage(String type, String msg) {
		return this.getSource().formatPositionLine(type, this.getSourcePosition(), msg);
	}

	@Override
	public final String formatDebugSourceMessage(String msg) {
		return this.source.formatDebugPositionMessage(this.getSourcePosition(), msg);
	}

	/**
	 * Create new input stream
	 * 
	 * @return SourceContext
	 */

	public final SourceContext newSourceContext() {
		return SourceContext.newStringContext(this.getSource().getResourceName(), this.getSource().linenum(this.getSourcePosition()), this.toText());
	}

	public final boolean containsToken(String token) {
		for (E sub : this) {
			if (sub.containsToken(token)) {
				return true;
			}
		}
		return token.equals(toText());
	}

}
