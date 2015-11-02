package nez.ast.transducer;

import nez.Grammar;
import nez.Strategy;
import nez.ast.Tree;
import nez.lang.Expression;

public class NezExpressionTransducer extends GrammarVisitorMap<ExpressionTransducerVisitor> implements ExpressionTransducer {

	public NezExpressionTransducer(Grammar grammar, Strategy strategy) {
		super(grammar, strategy);
		init(NezExpressionTransducer.class, new Undefined());
	}

	@Override
	public Expression newInstance(Tree<?> node) {
		return newInstance(node, null);
	}

	public Expression newInstance(Tree<?> node, Expression next) {
		this.find(key(node)).accept(node, next);
		return null;
	}

	public class Undefined implements ExpressionTransducerVisitor {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			throw new TransducerException(node, "NezExpressionTransducer: undefined " + node);
		}
	}

}
