package nez.expr;
import nez.ast.SourcePosition;
import nez.util.UList;

public abstract class SequentialExpression extends Expression {
	Expression[] inners;
	SequentialExpression(SourcePosition s, UList<Expression> list, int size) {
		super(s);
		this.inners = new Expression[size];
		for(int i = 0; i < size; i++) {
			this.inners[i] = list.get(i);
		}
	}
	@Override
	public final int size() {
		return this.inners.length;
	}
	@Override
	public final Expression get(int index) {
		return this.inners[index];
	}
	@Override
	public Expression set(int index, Expression e) {
		Expression oldExpresion = this.inners[index];
		this.inners[index] = e;
		return oldExpresion;
	}
//	public final void swap(int i, int j) {
//		Expression e = this.inners[i];
//		this.inners[i] = this.inners[j];
//		this.inners[j] = e;
//	}
	protected final UList<Expression> newList() {
		return new UList<Expression>(new Expression[this.size()]);
	}
	
//	@Override
//	public boolean checkAlwaysConsumed(String startNonTerminal, UList<String> stack) {
//		for(AbstractExpression e: this) {
//			if(e.checkAlwaysConsumed(startNonTerminal, stack)) {
//				return true;
//			}
//		}
//		return false;
//	}
//
//	@Override
//	public AbstractExpression removeNodeOperator() {
//		UList<AbstractExpression> l = new UList<AbstractExpression>(new AbstractExpression[this.size()]);
//		for(AbstractExpression e : this) {
//			AbstractExpression.addSequence(l, e.removeNodeOperator());
//		}
//		return AbstractExpression.newSequence(l);
//	}
}
