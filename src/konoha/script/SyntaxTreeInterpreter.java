package konoha.script;

public interface SyntaxTreeInterpreter {
	public Object accept(TypedTree node);
}
