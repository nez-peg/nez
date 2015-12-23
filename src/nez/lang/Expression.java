package nez.lang;

import java.util.AbstractList;
import java.util.HashMap;

import nez.ast.SourceLocation;
import nez.lang.expr.Cany;
import nez.lang.expr.Cbyte;
import nez.lang.expr.Cset;
import nez.lang.expr.ExpressionCommons;
import nez.lang.expr.NonTerminal;
import nez.lang.expr.Treplace;
import nez.lang.expr.Ttag;
import nez.util.UList;

public abstract class Expression extends AbstractList<Expression> {
	SourceLocation s = null;

	protected Expression() {
		this(null);
	}

	protected Expression(SourceLocation s) {
		this.s = s;
	}

	protected final void set(SourceLocation s) {
		this.s = s;
	}

	public final SourceLocation getSourceLocation() {
		return this.s;
	}

	public Expression getFirst() {
		return this;
	}

	public Expression getNext() {
		return null;
	}

	@Override
	public final String toString() {
		StringBuilder sb = new StringBuilder();
		format(sb);
		return sb.toString();
	}

	public abstract void format(StringBuilder sb);

	public final String getPredicate() {
		return this.getClass().getSimpleName().substring(1);
	}

	public abstract boolean isConsumed();

	public abstract Object visit(Expression.Visitor v, Object a);

	// boolean setOuterLefted(Expression outer) {
	// return false;
	// }

	public abstract short acceptByte(int ch);

	// test

	public static final boolean isByteConsumed(Expression e) {
		return (e instanceof Cbyte || e instanceof Cset || e instanceof Cany);
	}

	public static final boolean isPositionIndependentOperation(Expression e) {
		return (e instanceof Ttag || e instanceof Treplace);
	}

	// convinient interface

	public final Expression newEmpty() {
		return ExpressionCommons.newEmpty(this.getSourceLocation());
	}

	public final Expression newFailure() {
		return ExpressionCommons.newFailure(this.getSourceLocation());
	}

	public final Expression newCset(boolean isBinary, boolean[] byteMap) {
		return ExpressionCommons.newCset(this.getSourceLocation(), isBinary, byteMap);
	}

	public final Expression newSequence(Expression e, Expression e2) {
		return ExpressionCommons.newPsequence(this.getSourceLocation(), e, e2);
	}

	public final Expression newSequence(UList<Expression> l) {
		return ExpressionCommons.newPsequence(this.getSourceLocation(), l);
	}

	public final Expression newChoice(Expression e, Expression e2) {
		return ExpressionCommons.newPchoice(this.getSourceLocation(), e, e2);
	}

	public final Expression newChoice(UList<Expression> l) {
		return ExpressionCommons.newPchoice(this.getSourceLocation(), l);
	}

	/* static class */

	public static interface Conditional {

	}

	public static interface Contextual {

	}

	public static abstract class Visitor {

		protected HashMap<String, Object> visited = null;

		public final Object lookup(String uname) {
			if (visited != null) {
				return visited.get(uname);
			}
			return null;
		}

		public final void memo(String uname, Object o) {
			if (visited == null) {
				visited = new HashMap<>();
			}
			visited.put(uname, o);
		}

		public final boolean isVisited(String uname) {
			if (visited != null) {
				visited.containsKey(uname);
			}
			return false;
		}

		public final void visited(String uname) {
			memo(uname, uname);
		}

		public final void clear() {
			if (visited != null) {
				visited.clear();
			}
		}

		public abstract Object visitNonTerminal(NonTerminal e, Object a);

		public abstract Object visitEmpty(Nez.Empty e, Object a);

		public abstract Object visitFail(Nez.Fail e, Object a);

		public abstract Object visitByte(Nez.Byte e, Object a);

		public abstract Object visitByteSet(Nez.ByteSet e, Object a);

		public abstract Object visitAny(Nez.Any e, Object a);

		public abstract Object visitString(Nez.String e, Object a);

		public abstract Object visitPair(Nez.Pair e, Object a);

		// public abstract Object visitSequence(Nez.Sequence e, Object a);

		public abstract Object visitChoice(Nez.Choice e, Object a);

		public abstract Object visitOption(Nez.Option e, Object a);

		public abstract Object visitZeroMore(Nez.ZeroMore e, Object a);

		public abstract Object visitOneMore(Nez.OneMore e, Object a);

		public abstract Object visitAnd(Nez.And e, Object a);

		public abstract Object visitNot(Nez.Not e, Object a);

		public abstract Object visitPreNew(Nez.PreNew e, Object a);

		public abstract Object visitLeftFold(Nez.LeftFold e, Object a);

		public abstract Object visitLink(Nez.Link e, Object a);

		public abstract Object visitTag(Nez.Tag e, Object a);

		public abstract Object visitReplace(Nez.Replace e, Object a);

		public abstract Object visitNew(Nez.New e, Object a);

		public abstract Object visitDetree(Nez.Detree e, Object a);

		public abstract Object visitBlockScope(Nez.BlockScope e, Object a);

		public abstract Object visitLocalScope(Nez.LocalScope e, Object a);

		public abstract Object visitSymbolAction(Nez.SymbolAction e, Object a);

		public abstract Object visitSymbolPredicate(Nez.SymbolPredicate e, Object a);

		public abstract Object visitSymbolMatch(Nez.SymbolMatch e, Object a);

		public abstract Object visitSymbolExists(Nez.SymbolExists e, Object a);

		public abstract Object visitIf(Nez.If e, Object a);

		public abstract Object visitOn(Nez.On e, Object a);

		// public abstract Object visitSetCount(Nez.SetCount e, Object a);
		//
		// public abstract Object visitCount(Nez.Count e, Object a);

		public Object visitExtended(Expression e, Object a) {
			return a;
		}

	}

}
