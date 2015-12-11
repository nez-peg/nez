package nez.lang.ast;

import nez.ast.Transducer;
import nez.ast.Tree;
import nez.lang.Expression;

public interface ExpressionConstructor extends Transducer {
	@Override
	public Expression newInstance(Tree<?> node);
}
