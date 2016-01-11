package nez.tool.parser;

import nez.ast.CommonTree;
import nez.ast.Source;
import nez.ast.Symbol;
import nez.ast.Tree;
import nez.util.StringUtils;

public class Context {
	public int pos = 0;
	public Tree<?> left;

	public Context(String s) {
		byte[] b = StringUtils.toUtf8(s);
		inputs = new byte[b.length + 1];
		length = b.length;
		System.arraycopy(b, 0, inputs, 0, b.length);
		this.pos = 0;
		this.left = new CommonTree();
	}

	private Source source;
	private byte[] inputs;
	private int length;

	public final boolean eof() {
		return !(pos < length);
	}

	public final int read() {
		return inputs[pos++] & 0xff;
	}

	public final int prefetch() {
		return inputs[pos] & 0xff;
	}

	public final void move(int shift) {
		pos += shift;
	}

	public final boolean match(byte[] b, int len) {
		return true;
	}

	// AST

	enum Operation {
		Link, Tag, Replace, New;
	}

	static class AstLog {
		Operation op;
		// int debugId;
		int pos;
		Symbol label;
		Object value;
		AstLog prev;
		AstLog next;
	}

	private AstLog last = null;
	private AstLog unused = null;

	private void log(Operation op, int pos, Symbol label, Object value) {
		AstLog l;
		if (this.unused == null) {
			l = new AstLog();
		} else {
			l = this.unused;
			this.unused = l.next;
		}
		// l.debugId = last.debugId + 1;
		l.op = op;
		l.pos = pos;
		l.label = label;
		l.value = value;
		l.prev = last;
		l.next = null;
		last.next = l;
		last = l;
	}

	public final void beginTree(int shift) {
		log(Operation.New, pos + shift, null, null);
	}

	public final void linkTree(Tree<?> parent, Symbol label) {
		log(Operation.Link, 0, label, left);
	}

	public final void tagTree(Symbol tag) {
		log(Operation.Tag, 0, null, tag);
	}

	public final void valueTree(String value) {
		log(Operation.Replace, 0, null, value);
	}

	public final void foldTree(int shift, Symbol label) {
		log(Operation.New, pos + shift, null, null);
		log(Operation.Link, 0, label, left);
	}

	public final void endTree(Symbol tag, String value, int shift) {
		int objectSize = 0;
		AstLog start;
		for (start = last; start.op != Operation.New; start = start.prev) {
			switch (start.op) {
			case Link:
				objectSize++;
				break;
			case Tag:
				if (tag != null) {
					tag = (Symbol) start.value;
				}
				break;
			case Replace:
				if (value != null) {
					value = (String) start.value;
				}
				break;
			case New:
				break;
			}
		}
		left = left.newInstance(tag, source, start.pos, (pos + shift - start.pos), objectSize, value);
		if (objectSize > 0) {
			int n = 0;
			for (AstLog cur = start; cur != null; cur = cur.next) {
				if (cur.op == Operation.Link) {
					left.link(n++, cur.label, cur.value);
					cur.value = null;
				}
			}
		}
		this.rollback(start.prev);
	}

	public final AstLog save() {
		return last;
	}

	public final void rollback(AstLog save) {
		if (save != last) {
			last.next = this.unused;
			this.unused = save.next;
			save.next = null;
			this.last = save;
		}
	}

	// Memoization

	// Symbol Table

}
