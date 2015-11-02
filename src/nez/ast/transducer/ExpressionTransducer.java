package nez.ast.transducer;

import nez.ast.Transducer;
import nez.ast.Tree;
import nez.lang.Expression;

public interface ExpressionTransducer extends Transducer {
	@Override
	public Expression newInstance(Tree<?> node);
}
