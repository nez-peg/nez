package nez.bx;

import nez.ast.Tree;

public class StringTransducerCombinator {
	public StringTransducer make(StringTransducer... list) {
		StringTransducer head = null;
		StringTransducer prev = null;
		for (StringTransducer st : list) {
			if (head == null) {
				head = st;
				prev = head;
			} else {
				prev.next = st;
				prev = st;
			}
		}
		return head;
	}

	public StringTransducer S(String text) {
		return new TextualStringTransducer(text);
	}

	public StringTransducer Node(int index) {
		return new NodeStringTransducer(index);
	}

	public StringTransducer RangeNode(int start, StringTransducer st, int end) {
		return new RangeNodeStringTransducer(start, st, end);
	}

	public StringTransducer Asis() {
		return new StringTransducer();
	}

	public StringTransducer Empty() {
		return new EmptyAction();
	}

	public StringTransducer Inc() {
		return new IncAction();
	}

	public StringTransducer Dec() {
		return new IncAction();
	}

	public StringTransducer NL() {
		return new IndentAction();
	}
}

class TextualStringTransducer extends StringTransducer {
	final String text;

	TextualStringTransducer(String text) {
		this.text = text;
	}

	@Override
	protected <E extends Tree<E>> void formatTo(Tree<E> node, StringTransducerBuilder stream) {
		stream.write(text);
	}
}

class NodeStringTransducer extends StringTransducer {
	final int index;

	NodeStringTransducer(int index) {
		this.index = index;
	}

	public static int index(int index, int size) {
		return (index < 0) ? size + index + 1 : index;
	}

	@Override
	protected <E extends Tree<E>> void formatTo(Tree<E> node, StringTransducerBuilder stream) {
		int size = node.size();
		int index = NodeStringTransducer.index(this.index, size);
		if (0 <= index && index < size) {
			Tree<E> sub = node.get(index);
			StringTransducer st = stream.lookup(sub);
			st.trasformTo(sub, stream);
		}
	}
}

class RangeNodeStringTransducer extends StringTransducer {
	int start;
	StringTransducer delim;
	int end;

	RangeNodeStringTransducer(int s, StringTransducer delim, int e) {
		this.start = s;
		this.delim = delim;
		this.end = e;
	}

	@Override
	protected <E extends Tree<E>> void formatTo(Tree<E> node, StringTransducerBuilder stream) {
		int size = node.size();
		int s = NodeStringTransducer.index(this.start, size);
		int e = NodeStringTransducer.index(this.end, size);
		if (e > size) {
			e = size;
		}
		for (int i = s; i < e; i++) {
			if (i > s) {
				delim.trasformTo(node, stream);
			}
			Tree<E> sub = node.get(i);
			StringTransducer st = stream.lookup(sub);
			st.trasformTo(sub, stream);
		}
	}
}

class EmptyAction extends StringTransducer {
	@Override
	protected <E extends Tree<E>> void formatTo(Tree<E> node, StringTransducerBuilder stream) {
	}
}

class IndentAction extends StringTransducer {
	@Override
	protected <E extends Tree<E>> void formatTo(Tree<E> node, StringTransducerBuilder stream) {
		stream.writeNewLineIndent();
	}
}

class IncAction extends StringTransducer {
	@Override
	protected <E extends Tree<E>> void formatTo(Tree<E> node, StringTransducerBuilder stream) {
		stream.incIndent();
	}
}

class DecAction extends StringTransducer {
	@Override
	protected <E extends Tree<E>> void formatTo(Tree<E> node, StringTransducerBuilder stream) {
		stream.decIndent();
	}
}