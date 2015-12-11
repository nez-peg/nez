package nez.lang.ast;

import nez.ast.Tree;

@SuppressWarnings("serial")
public class TransducerException extends RuntimeException {
	Tree<?> node;

	public TransducerException(Tree<?> node, String fmt, Object... args) {
		super(node.formatSourceMessage("error", String.format(fmt, args)));
		this.node = node;
	}
}