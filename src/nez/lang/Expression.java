package nez.lang;

import java.util.AbstractList;

import nez.ast.SourcePosition;
import nez.lang.expr.Cany;
import nez.lang.expr.Cbyte;
import nez.lang.expr.Cset;
import nez.lang.expr.ExpressionCommons;
import nez.lang.expr.Treplace;
import nez.lang.expr.Ttag;
import nez.util.UList;
import nez.vm.Instruction;
import nez.vm.NezEncoder;

public abstract class Expression extends AbstractList<Expression> {

	SourcePosition s = null;

	// int internId = 0;

	protected Expression(SourcePosition s) {
		this.s = s;
		// this.internId = 0;
	}

	public final SourcePosition getSourcePosition() {
		return this.s;
	}

	public Expression getFirst() {
		return this;
	}

	public Expression getNext() {
		return null;
	}

	public abstract boolean equalsExpression(Expression o);

	public abstract void format(StringBuilder sb);

	@Deprecated
	public final int getId() {
		return 0;
		// return this.internId;
	}

	@Deprecated
	public String getPredicate() {
		return this.getClass().getSimpleName();
	}

	// public final boolean isInterned() {
	// return (this.internId > 0);
	// }

	// final Expression intern() {
	// return ExpressionCommons.intern(this);
	// }
	//
	//
	// public String key() {
	// return this.getPredicate();
	// }

	public abstract boolean isConsumed();

	public abstract Expression reshape(ExpressionTransducer m);

	// boolean setOuterLefted(Expression outer) {
	// return false;
	// }

	public final int inferTypestate() {
		return this.inferTypestate(null);
	}

	public abstract int inferTypestate(Visa v);

	public abstract short acceptByte(int ch);

	public abstract Instruction encode(NezEncoder bc, Instruction next, Instruction failjump);

	// test

	public static final boolean isByteConsumed(Expression e) {
		return (e instanceof Cbyte || e instanceof Cset || e instanceof Cany);
	}

	public static final boolean isPositionIndependentOperation(Expression e) {
		return (e instanceof Ttag || e instanceof Treplace);
	}

	// convinient interface

	public final Expression newEmpty() {
		return ExpressionCommons.newEmpty(this.getSourcePosition());
	}

	public final Expression newFailure() {
		return ExpressionCommons.newFailure(this.getSourcePosition());
	}

	public final Expression newByteMap(boolean isBinary, boolean[] byteMap) {
		return ExpressionCommons.newByteMap(this.getSourcePosition(), isBinary, byteMap);
	}

	public final Expression newSequence(Expression e, Expression e2) {
		return ExpressionCommons.newSequence(this.getSourcePosition(), e, e2);
	}

	public final Expression newSequence(UList<Expression> l) {
		return ExpressionCommons.newSequence(this.getSourcePosition(), l);
	}

	public final Expression newChoice(Expression e, Expression e2) {
		return ExpressionCommons.newChoice(this.getSourcePosition(), e, e2);
	}

	public final Expression newChoice(UList<Expression> l) {
		return ExpressionCommons.newChoice(this.getSourcePosition(), l);
	}

}
