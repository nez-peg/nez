package nez.lang.expr;

import nez.ast.SourcePosition;
import nez.lang.Expression;
import nez.lang.ExpressionVisitor;
import nez.lang.PossibleAcceptance;
import nez.lang.Typestate;
import nez.lang.Visa;
import nez.util.UList;

public class Pchoice extends ExpressionCommons {
	Expression[] inners;

	Pchoice(SourcePosition s, UList<Expression> l, int size) {
		super(s);
		this.inners = new Expression[size];
		for (int i = 0; i < size; i++) {
			this.inners[i] = l.get(i);
		}
		this.reduced = size;
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

	@Override
	public final boolean equalsExpression(Expression o) {
		if (o instanceof Pchoice && this.size() == o.size()) {
			for (int i = 0; i < this.size(); i++) {
				if (!this.get(i).equalsExpression(o.get(i))) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public final void format(StringBuilder sb) {
		for (int i = 0; i < this.size(); i++) {
			if (i > 0) {
				sb.append(" / ");
			}
			this.get(i).format(sb);
		}
	}

	@Override
	public Object visit(ExpressionVisitor v, Object a) {
		return v.visitPchoice(this, a);
	}

	@Override
	public boolean isConsumed() {
		boolean afterAll = true;
		for (Expression e : this) {
			if (!e.isConsumed()) {
				afterAll = false;
			}
		}
		return afterAll;
	}

	@Override
	public int inferTypestate(Visa v) {
		int t = Typestate.BooleanType;
		for (Expression s : this) {
			t = s.inferTypestate(v);
			if (t == Typestate.ObjectType || t == Typestate.OperationType) {
				return t;
			}
		}
		return t;
	}

	@Override
	public short acceptByte(int ch) {
		boolean hasUnconsumed = false;
		for (int i = 0; i < this.size(); i++) {
			short r = this.get(i).acceptByte(ch);
			if (r == PossibleAcceptance.Accept) {
				return r;
			}
			if (r == PossibleAcceptance.Unconsumed) {
				hasUnconsumed = true;
			}
		}
		return hasUnconsumed ? PossibleAcceptance.Unconsumed : PossibleAcceptance.Reject;
	}

	/* optimized */

	public boolean isTrieTree = false;
	public Expression[] predictedCase = null;
	public float reduced;
	public Expression[] firstInners = null;
	private final static Expression[] optimized = new Expression[0];

	public boolean isOptimized() {
		return this.firstInners != null;
	}

	public void setOptimized() {
		this.firstInners = optimized;
	}

}
