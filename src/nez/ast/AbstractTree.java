package nez.ast;

import java.util.AbstractList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import nez.SourceContext;
import nez.util.StringUtils;

public abstract class AbstractTree<E extends AbstractTree<E>> extends AbstractList<E> {
	protected Tag             tag;
	protected Source          source;
	protected int             pos;
	protected int             length;
	protected Object          value;
	protected AbstractTree<E> parent = null;
	protected E[]             subTree;
	
	
	protected AbstractTree(Tag tag, Source source, long pos, int len, E[] subTree, Object value) {
		this.tag        = tag;
		this.source     = source;
		this.pos        = (int)pos;
		this.length     = len;
		this.subTree    = subTree;
		this.value      = value;
	}
	
	protected abstract E dupImpl();
	
	public final E dup() {
		E t = dupImpl();
		if(this.subTree != null) {
			for(int i = 0; i < subTree.length; i++) {
				if(this.subTree[i] != null) {
					t.subTree[i] = this.subTree[i].dup();
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
		if(this.subTree == null) {
			return 0;
		}
		return this.subTree.length;
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
	
	void link(int index, E child) {
		subTree[index] = child;
	}

	public final boolean is(Tag t) {
		return t == this.getTag();
	}
	
	public final String formatSourceMessage(String type, String msg) {
		return this.getSource().formatPositionLine(type, this.getSourcePosition(), msg);
	}
	
	/**
	 * Create new input stream 
	 * @return SourceContext
	 */
	
	public final SourceContext newSourceContext() {
		return SourceContext.newStringSourceContext(this.getSource().getResourceName(), 
				this.getSource().linenum(this.getSourcePosition()), this.getText());
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		this.stringfy("", sb);
		return sb.toString();
	}

	protected final Object getValue() {
		return this.value;
	}
	
	public final String getText() {
		if(this.value != null) {
			return this.value.toString();
		}
		if(this.source != null) {
			this.value = this.source.substring(this.getSourcePosition(), this.getSourcePosition() + this.length);
			return this.value.toString();
		}
		return "";
	}
	
	public void stringfy(String indent, StringBuilder sb) {
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

	public final String textAt(int index, String defaultValue) {
		if(index < this.size()) {
			return this.get(index).getText();
		}
		return defaultValue;
	}
	
	public final int getInt(int defvalue) {
		if(this.value instanceof Number) {
			return ((Number)this.value).intValue();
		}
		try {
			String s = this.getText();
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

	public final int getIntAt(int index, int defvalue) {
		if(index < this.size()) {
			return this.get(index).getInt(defvalue);
		}
		return defvalue;
	}

	public final boolean containsToken(String token) {
		for(E sub : this) {
			if(sub.containsToken(token)) {
				return true;
			}
		}
		return token.equals(getText());
	}

	
//	// 
//	
//	


}
