package nez.ast.script;

public interface SyntaxTreeInterpreter {
	public Object accept(TypedTree node);
}
