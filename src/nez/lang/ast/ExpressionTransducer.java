package nez.lang.ast;

import nez.ast.Tree;
import nez.lang.Expression;

public interface ExpressionTransducer {
	Expression accept(Tree<?> node, Expression next);
}
