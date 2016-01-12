package nez.ast.string;

import nez.ast.Tree;
import nez.ast.Symbol;
import nez.util.StringUtils;

public class StringTransducer {

	StringTransducer next;

	protected <E extends Tree<E>> void formatTo(Tree<E> node, StringTransducerBuilder stream) {
		stream.write(node.toText());
	}

	public final <E extends Tree<E>> void trasformTo(Tree<E> node, StringTransducerBuilder stream) {
		StringTransducer st = this;
		while (st != null) {
			st.formatTo(node, stream);
			st = st.next;
		}
	}

	//

	private static final Symbol FormatTag = Symbol.unique("Format");
	private static final Symbol NameTag = Symbol.unique("Name");
	private static final Symbol ListTag = Symbol.unique("List");
	private static final Symbol IntTag = Symbol.unique("Integer");

	public final static <E extends Tree<E>> StringTransducer parseStringTransducer(Tree<E> node) {
		if (node.is(NameTag)) {
			return newActionStringTransducer(node.toText());
		}
		if (node.is(IntTag)) {
			return new NodeStringTransducer(node.toInt(0));
		}
		if (node.is(ListTag)) {
			StringTransducer head = parseStringTransducer(node.get(0));
			StringTransducer prev = head;
			for (int i = 1; i < node.size(); i++) {
				StringTransducer st = parseStringTransducer(node.get(i));
				prev.next = st;
				prev = st;
			}
			return head;
		}
		if (node.is(FormatTag)) {
			int s = StringUtils.parseInt(node.getText(0, "*"), -1);
			StringTransducer delim = parseStringTransducer(node.get(1));
			int e = StringUtils.parseInt(node.getText(2, "*"), -1);
			return new RangeNodeStringTransducer(s, delim, e);
		}
		return new TextualStringTransducer(node.toText());
	}

	public final static <E extends Tree<E>> StringTransducer newActionStringTransducer(String command) {
		switch (command) {
		case "NL":
			return new IndentAction();
		case "inc":
			return new IncAction();
		case "dec":
			return new DecAction();
		}
		return new TextualStringTransducer("${" + command + "}");
	}

}
