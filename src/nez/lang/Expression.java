package nez.lang;

import java.util.AbstractList;
import java.util.HashMap;

import nez.ast.SourcePosition;
import nez.lang.expr.Cany;
import nez.lang.expr.Cbyte;
import nez.lang.expr.Cmulti;
import nez.lang.expr.Cset;
import nez.lang.expr.ExpressionCommons;
import nez.lang.expr.NonTerminal;
import nez.lang.expr.Pand;
import nez.lang.expr.Pchoice;
import nez.lang.expr.Pempty;
import nez.lang.expr.Pfail;
import nez.lang.expr.Pnot;
import nez.lang.expr.Pone;
import nez.lang.expr.Poption;
import nez.lang.expr.Psequence;
import nez.lang.expr.Pzero;
import nez.lang.expr.Tcapture;
import nez.lang.expr.Tdetree;
import nez.lang.expr.Tlfold;
import nez.lang.expr.Tlink;
import nez.lang.expr.Tnew;
import nez.lang.expr.Treplace;
import nez.lang.expr.Ttag;
import nez.lang.expr.Xblock;
import nez.lang.expr.Xexists;
import nez.lang.expr.Xif;
import nez.lang.expr.Xindent;
import nez.lang.expr.Xis;
import nez.lang.expr.Xlocal;
import nez.lang.expr.Xmatch;
import nez.lang.expr.Xon;
import nez.lang.expr.Xsymbol;
import nez.util.UList;

public abstract class Expression extends AbstractList<Expression> {
	SourcePosition s = null;

	protected Expression(SourcePosition s) {
		this.s = s;
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
	}

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
		return ExpressionCommons.newEmpty(this.getSourcePosition());
	}

	public final Expression newFailure() {
		return ExpressionCommons.newFailure(this.getSourcePosition());
	}

	public final Expression newCset(boolean isBinary, boolean[] byteMap) {
		return ExpressionCommons.newCset(this.getSourcePosition(), isBinary, byteMap);
	}

	public final Expression newSequence(Expression e, Expression e2) {
		return ExpressionCommons.newPsequence(this.getSourcePosition(), e, e2);
	}

	public final Expression newSequence(UList<Expression> l) {
		return ExpressionCommons.newPsequence(this.getSourcePosition(), l);
	}

	public final Expression newChoice(Expression e, Expression e2) {
		return ExpressionCommons.newPchoice(this.getSourcePosition(), e, e2);
	}

	public final Expression newChoice(UList<Expression> l) {
		return ExpressionCommons.newPchoice(this.getSourcePosition(), l);
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

		public abstract Object visitPempty(Pempty e, Object a);

		public abstract Object visitPfail(Pfail e, Object a);

		public abstract Object visitCbyte(Cbyte e, Object a);

		public abstract Object visitCset(Cset e, Object a);

		public abstract Object visitCany(Cany e, Object a);

		public abstract Object visitCmulti(Cmulti e, Object a);

		public abstract Object visitPsequence(Psequence e, Object a);

		public abstract Object visitPchoice(Pchoice e, Object a);

		public abstract Object visitPoption(Poption e, Object a);

		public abstract Object visitPzero(Pzero e, Object a);

		public abstract Object visitPone(Pone e, Object a);

		public abstract Object visitPand(Pand e, Object a);

		public abstract Object visitPnot(Pnot e, Object a);

		public abstract Object visitTnew(Tnew e, Object a);

		public abstract Object visitTlfold(Tlfold e, Object a);

		public abstract Object visitTlink(Tlink e, Object a);

		public abstract Object visitTtag(Ttag e, Object a);

		public abstract Object visitTreplace(Treplace e, Object a);

		public abstract Object visitTcapture(Tcapture e, Object a);

		public abstract Object visitTdetree(Tdetree e, Object a);

		public abstract Object visitXblock(Xblock e, Object a);

		public abstract Object visitXlocal(Xlocal e, Object a);

		public abstract Object visitXdef(Xsymbol e, Object a);

		public abstract Object visitXmatch(Xmatch e, Object a);

		public abstract Object visitXis(Xis e, Object a);

		public abstract Object visitXexists(Xexists e, Object a);

		public abstract Object visitXindent(Xindent e, Object a);

		public abstract Object visitXif(Xif e, Object a);

		public abstract Object visitXon(Xon e, Object a);

		public Object visitUndefined(Expression e, Object a) {
			return a;
		}

	}

}
