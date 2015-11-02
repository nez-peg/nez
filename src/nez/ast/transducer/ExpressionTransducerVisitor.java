package nez.ast.transducer;

import nez.ast.Tree;
import nez.lang.Expression;

public interface ExpressionTransducerVisitor {
	public Expression accept(Tree<?> node, Expression next);
}
