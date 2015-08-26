package nez.ast;

import java.util.AbstractList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import nez.SourceContext;
import nez.util.StringUtils;

public abstract class AbstractTree<E extends AbstractTree<E>> extends AbstractList<E> implements SourcePosition {
	private final static Tag[] EmptyLabels = new Tag[0];

	protected Tag             tag;
	protected Source          source;
	protected int             pos;
	protected int             length;
	protected Object          value;
	protected AbstractTree<E> parent = null;
	protected Tag[]           labels;
	protected E[]             subTree;
	
	protected AbstractTree(Tag tag, Source source, long pos, int len, E[] subTree, Object value) {
		this.tag        = tag;
		this.source     = source;
		this.pos        = (int)pos;
		this.length     = len;
		this.subTree    = subTree;
		this.value      = value;
		this.labels = (this.subTree != null) ? new Tag[this.subTree.length] : EmptyLabels;
	}
	
	protected abstract E dupImpl();
	
	public final E dup() {
		E t = dupImpl();
		if(this.subTree != null) {
			for(int i = 0; i < subTree.length; i++) {
				if(this.subTree[i] != null) {
					t.subTree[i] = this.subTree[i].dup();
					t.labels[i] = this.labels[i];
				}
			}
		}
		return t;
	}
	
	public final Tag getTag() {
		return this.tag;
	}

	public final AbstractTree<E> getParent() {
		return parent;
	}

	public final void setParent(AbstractTree<E> parent) {
		this.parent = parent;
	}
	
	public final Source getSource() {
		return this.source;
	}

	public final long getSourcePosition() {
		return this.pos;
	}

	protected final int getLength() {
		return this.length;
	}

	public int size() {
		return this.labels.length;
	}
	
	public final boolean isEmpty() {
		return this.size() == 0;
	}

	public final int count() {
		int c = 1;
		for(E t: this) {
			if(t != null) {
				c += t.count();
			}
		}
		return c;
	}
	
	public E get(int index) {
		return this.subTree[index];
	}

	public final E get(int index, E defaultValue) {
		if(index < this.size()) {
			return this.subTree[index];
		}
		return defaultValue;
	}
	
	@Override
	public final E set(int index, E node) {
		E oldValue = null;
		oldValue = this.subTree[index];
		this.subTree[index] = node;
		node.setParent(this);
		return oldValue;
	}

	public final void set(int index, Tag label, E node) {
		this.labels[index] = label;
		this.subTree[index] = node;
	}
	
	public final int indexOf(Tag label) {
		for(int i = 0; i < labels.length; i++) {
			if(labels[i] == label) {
				return i;
			}
		}
		return -1;
	}
	
	public final E get(Tag label) {
		for(int i = 0; i < labels.length; i++) {
			if(labels[i] == label) {
				return this.subTree[i];
			}
		}
		throw new RuntimeException("undefined label: " + label);
	}

	public final E get(Tag label, E defval) {
		for(int i = 0; i < labels.length; i++) {
			if(labels[i] == label) {
				return this.subTree[i];
			}
		}
		return defval;
	}

	public final void set(Tag label, E defval) {
		for(int i = 0; i < labels.length; i++) {
			if(labels[i] == label) {
				this.subTree[i] = defval;
				return;
			}
		}
	}

	

	public final boolean is(Tag t) {
		return t == this.getTag();
	}
	
	public final String formatSourceMessage(String type, String msg) {
		return this.getSource().formatPositionLine(type, this.getSourcePosition(), msg);
	}
	
	public final String formatDebugSourceMessage(String msg) {
		return this.source.formatDebugPositionMessage(this.getSourcePosition(), msg);
	}

	/**
	 * Create new input stream 
	 * @return SourceContext
	 */
	
	public final SourceContext newSourceContext() {
		return SourceContext.newStringSourceContext(this.getSource().getResourceName(), 
				this.getSource().linenum(this.getSourcePosition()), this.toText());
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		this.stringfy("", null, sb);
		return sb.toString();
	}

	protected final Object getValue() {
		return this.value;
	}
	
	public final String toText() {
		if(this.value != null) {
			return this.value.toString();
		}
		if(this.source != null) {
			this.value = this.source.substring(this.getSourcePosition(), this.getSourcePosition() + this.length);
			return this.value.toString();
		}
		return "";
	}
	
	public void stringfy(String indent, Tag label, StringBuilder sb) {
		sb.append("\n");
		sb.append(indent);
		if(label != null) {
			sb.append(label);
			sb.append(" ");
		}
		sb.append("#");
		sb.append(this.getTag().getName());
		sb.append("[");
		if(this.subTree == null) {
			sb.append(" ");
			StringUtils.formatQuoteString(sb, '\'', this.toText(), '\'');
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
					this.subTree[i].stringfy(nindent, this.labels[i], sb);
				}
			}
			sb.append("\n");
			sb.append(indent);
			sb.append("]");
		}
	}

	public final String getText(int index, String defval) {
		if(index < this.size()) {
			return this.get(index).toText();
		}
		return defval;
	}

	public final String getText(Tag label, String defval) {
		for(int i = 0; i < this.labels.length; i++) {
			if(labels[i] == label) {
				return getText(i, defval);
			}
		}
		return defval;
	}
	
	public final int toInt(int defvalue) {
		if(this.value instanceof Number) {
			return ((Number)this.value).intValue();
		}
		try {
			String s = this.toText();
			int num = Integer.parseInt(s);
			if(this.value == null) {
				this.value = new Integer(num);
			}
			return num;
		}
		catch(NumberFormatException e) {
		}
		return defvalue;
	}

	public final int getInt(int index, int defvalue) {
		if(index < this.size()) {
			return this.get(index).toInt(defvalue);
		}
		return defvalue;
	}

	public final int getInt(Tag label, int defvalue) {
		for(int i = 0; i < this.labels.length; i++) {
			if(labels[i] == label) {
				return getInt(i, defvalue);
			}
		}
		return defvalue;
	}

	
	
	
	public final boolean containsToken(String token) {
		for(E sub : this) {
			if(sub.containsToken(token)) {
				return true;
			}
		}
		return token.equals(toText());
	}

	
//	// 
//	
//	


}
