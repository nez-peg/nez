package nez.ast;

@SuppressWarnings("serial")
public class UndefinedVisitorMapException extends RuntimeException {
	Tree<?> node;

	public UndefinedVisitorMapException(Tree<?> node, String msg) {
		super(node.formatSourceMessage("error", msg));
		this.node = node;
	}

	public UndefinedVisitorMapException(Tree<?> node, String fmt, Object... args) {
		super(node.formatSourceMessage("error", String.format(fmt, args)));
		this.node = node;
	}
}