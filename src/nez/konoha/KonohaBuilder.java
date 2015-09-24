package nez.konoha;

import nez.ast.Tree;
import nez.ast.string.StringTransducer;
import nez.ast.string.StringTransducerBuilder;

public class KonohaBuilder implements StringTransducerBuilder {
	StringTransducer defaultTransducer = new StringTransducer();
	StringBuilder sb = new StringBuilder();
	int indent;

	@Override
	public <E extends Tree<E>> StringTransducer lookup(Tree<E> sub) {
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
