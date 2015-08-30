package nez.konoha;

import nez.ast.AbstractTree;
import nez.string.StringTransducer;
import nez.string.StringTransducerBuilder;

public class KonohaBuilder implements StringTransducerBuilder {
	StringTransducer defaultTransducer = new StringTransducer();
	StringBuilder sb = new StringBuilder();
	int indent;

	@Override
	public <E extends AbstractTree<E>> StringTransducer lookup(AbstractTree<E> sub) {
		KonohaTree node = (KonohaTree) sub;
		StringTransducer st = node.getStringTransducer();
		return st == null ? defaultTransducer : st;
	}

	@Override
	public void write(String text) {
		sb.append(text);
	}

	@Override
	public void writeNewLineIndent() {
		sb.append("\n");
		for (int i = 0; i < indent; i++) {
			sb.append("   ");
		}
	}

	@Override
	public void incIndent() {
		indent++;
	}

	@Override
	public void decIndent() {
		indent--;
	}

	public String toString() {
		return sb.toString();
	}

}
