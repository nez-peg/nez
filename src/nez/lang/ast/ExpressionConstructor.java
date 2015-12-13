package nez.lang.ast;

import nez.ast.Constructor;
import nez.ast.Tree;
import nez.lang.Expression;

public interface ExpressionConstructor extends Constructor {
	@Override
	public Expression newInstance(Tree<?> node);
}
