package nez.ast.script;

class TypeCheckerException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	TypedTree errorTree;

	TypeCheckerException(TypeSystem system, TypedTree node, String fmt, Object... args) {
		this.errorTree = system.newError(void.class, node, fmt, args);
	}
}
