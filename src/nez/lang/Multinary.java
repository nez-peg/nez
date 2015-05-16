package nez.lang;
import nez.ast.SourcePosition;
import nez.util.UList;

public abstract class Multinary extends Expression {
	Expression[] inners;
	Multinary(SourcePosition s, UList<Expression> list, int size) {
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
	protected final UList<Expression> newList() {
		return new UList<Expression>(new Expression[this.size()]);
	}
	
}
