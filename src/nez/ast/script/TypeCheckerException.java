package nez.ast.script;

import java.lang.reflect.Method;

public class TypeCheckerException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	TypedTree errorTree;

	TypeCheckerException(TypedTree node, String fmt, Object... args) {
		this.errorTree = newErrorTree(node, fmt, args);
	}

	private TypedTree newErrorTree(TypedTree node, String fmt, Object... args) {
		TypedTree newnode = node.newInstance(CommonSymbols._Error, 1, null);
		String msg = node.formatSourceMessage("error", String.format(fmt, args));
		newnode.set(0, CommonSymbols._msg, node.newStringConst(msg));
		// context.log(msg);
		newnode.setMethod(Hint.StaticInvocation, errorMethod(), null);
		return newnode;
	}

	private static Method StaticErrorMethod = null;

	static Method errorMethod() {
		if (StaticErrorMethod == null) {
			StaticErrorMethod = Reflector.load(TypeCheckerException.class, "error", String.class);
		}
		return StaticErrorMethod;
	}

	public final static void error(String msg) {
		throw new ScriptRuntimeException(msg);
	}

	@Override
	public final String getMessage() {
		return this.errorTree.getText(CommonSymbols._msg, "error");
	}

}
