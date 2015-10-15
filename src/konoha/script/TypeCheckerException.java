package konoha.script;

import konoha.message.Message;

public class TypeCheckerException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	TypedTree errorTree;

	TypeCheckerException(TypedTree node, Message fmt, Object... args) {
		this.errorTree = newErrorTree(node, fmt.toString(), args);
	}

	TypeCheckerException(TypedTree node, String fmt, Object... args) {
		this.errorTree = newErrorTree(node, fmt, args);
	}

	private TypedTree newErrorTree(TypedTree node, String fmt, Object... args) {
		TypedTree newnode = node.newInstance(CommonSymbols._Error, 1, null);
		String msg = node.formatSourceMessage("error", String.format(fmt, args));
		newnode.set(0, CommonSymbols._msg, node.newStringConst(msg));
		newnode.setInterface(Hint.StaticInvocation2, KonohaRuntime.error());
		return newnode;
	}

	@Override
	public final String getMessage() {
		return this.errorTree.getText(CommonSymbols._msg, "error");
	}

}
