package nez.ast.script;

public class TypeCheckerException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	TypedTree errorTree;

	TypeCheckerException(TypedTree node, TypedTree errorTree) {
		this.errorTree = errorTree;
	}

	@Override
	public final String getMessage() {
		return this.errorTree.getText(CommonSymbols._msg, "error");
	}

}
