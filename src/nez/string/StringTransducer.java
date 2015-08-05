package nez.string;

import nez.ast.AbstractTree;
import nez.ast.Tag;
import nez.util.StringUtils;

public class StringTransducer {

	StringTransducer next;

	protected <E extends AbstractTree<E>> void formatTo(AbstractTree<E> node, StringTransducerBuilder stream) {
		stream.write(node.getText());
	}

	public final <E extends AbstractTree<E>> void trasformTo(AbstractTree<E> node, StringTransducerBuilder stream) {
		StringTransducer st = this;
		while(st != null) {
			st.formatTo(node, stream);
			st = st.next;
		}
	}

	//

	private static final Tag FormatTag = Tag.tag("Format");
	private static final Tag NameTag = Tag.tag("Name");
	private static final Tag ListTag = Tag.tag("List");
	private static final Tag IntTag = Tag.tag("Integer");

	public final static <E extends AbstractTree<E>> StringTransducer parseStringTransducer(AbstractTree<E> node) {
		if(node.is(NameTag)) {
			return newActionStringTransducer(node.getText());
		}
		if(node.is(IntTag)) {
			return new NodeStringTransducer(node.getInt(0));
		}
		if(node.is(ListTag)) {
			StringTransducer head = parseStringTransducer(node.get(0));
			StringTransducer prev = head;
			for(int i = 1; i < node.size(); i++) {
				StringTransducer st = parseStringTransducer(node.get(i));
				prev.next = st;
				prev = st;
			}
			return head;
		}
		if(node.is(FormatTag)) {
			int s = StringUtils.parseInt(node.textAt(0, "*"), -1);
			StringTransducer delim = parseStringTransducer(node.get(1));
			int e = StringUtils.parseInt(node.textAt(2, "*"), -1);
			return new RangeNodeStringTransducer(s, delim, e);
		}
		return new TextualStringTransducer(node.getText());
	}

	public final static <E extends AbstractTree<E>> StringTransducer newActionStringTransducer(String command) {
		switch(command) {
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
