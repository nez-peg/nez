package nez.lang;

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
import nez.main.Verbose;
import nez.util.UList;

public class GrammarTransducer {

	public Expression reshapeInner(Expression inner) {
		push(inner);
		Expression inner2 = inner.reshape(this);
		pop(inner);
		return inner2;
	}

	protected void push(Expression inner) {
	}

	protected void pop(Expression inner) {
	}

	public Production reshapeProduction(Production p) {
		p.setExpression(this.reshapeInner(p.getExpression()));
		return p;
	}

	// protected final Expression empty(Expression e) {
	// return ExpressionCommons.newEmpty(null);
	// }
	//
	// protected final Expression fail(Expression e) {
	// return ExpressionCommons.newFailure(null);
	// }

	public Expression reshapePempty(Pempty e) {
		return e; // immutable
	}

	public Expression reshapePfail(Pfail e) {
		return e; // immutable
	}

	public Expression reshapeCbyte(Cbyte e) {
		return e; // immutable
	}

	public Expression reshapeCset(Cset e) {
		return e; // immutable
	}

	public Expression reshapeCany(Cany e) {
		return e; // immutable
	}

	public Expression reshapeCmulti(Cmulti e) {
		return e; // immutable
	}

	public Expression reshapeNonTerminal(NonTerminal e) {
		e.newNonTerminal(e.getLocalName());
		return e;
	}

	public Expression reshapePsequence(Psequence e) {
		Expression first = e.getFirst();
		push(first);
		first = first.reshape(this);
		Expression next = e.getNext();
		push(next);
		next = next.reshape(this);
		pop(e.getNext());
		pop(e.getFirst());
		if (first instanceof Pempty) {
			return next;
		}
		if (first instanceof Pfail) {
			return first;
		}
		return e.newSequence(first, next);
	}

	public Expression reshapePchoice(Pchoice e) {
		UList<Expression> l = ExpressionCommons.newList(e.size());
		for (Expression sub : e) {
			ExpressionCommons.addChoice(l, this.reshapeInner(sub));
		}
		return e.newChoice(l);
	}

	public Expression reshapePoption(Poption e) {
		return ExpressionCommons.newPoption(e.getSourcePosition(), reshapeInner(e.get(0)));
	}

	public Expression reshapePzero(Pzero e) {
		return ExpressionCommons.newPzero(e.getSourcePosition(), reshapeInner(e.get(0)));
	}

	public Expression reshapePone(Pone e) {
		return ExpressionCommons.newPone(e.getSourcePosition(), reshapeInner(e.get(0)));
	}

	public Expression reshapePand(Pand e) {
		return ExpressionCommons.newPand(e.getSourcePosition(), reshapeInner(e.get(0)));
	}

	public Expression reshapePnot(Pnot e) {
		return ExpressionCommons.newPnot(e.getSourcePosition(), reshapeInner(e.get(0)));
	}

	public Expression reshapeTnew(Tnew e) {
		return ExpressionCommons.newTnew(e.getSourcePosition(), e.shift);
	}

	public Expression reshapeTlfold(Tlfold e) {
		return ExpressionCommons.newTlfold(e.getSourcePosition(), e.getLabel(), e.shift);
	}

	public Expression reshapeTlink(Tlink e) {
		return ExpressionCommons.newTlink(e.getSourcePosition(), e.getLabel(), reshapeInner(e.get(0)));
	}

	public Expression reshapeTtag(Ttag e) {
		return e; // immutable
	}

	public Expression reshapeTreplace(Treplace e) {
		return e; // immutable
	}

	public Expression reshapeTcapture(Tcapture e) {
		return ExpressionCommons.newTcapture(e.getSourcePosition(), e.shift);
	}

	public Expression reshapeTdetree(Tdetree e) {
		return ExpressionCommons.newTdetree(e.getSourcePosition(), reshapeInner(e.get(0)));
	}

	public Expression reshapeXblock(Xblock e) {
		return ExpressionCommons.newXblock(e.getSourcePosition(), reshapeInner(e.get(0)));
	}

	public Expression reshapeXlocal(Xlocal e) {
		return ExpressionCommons.newXlocal(e.getSourcePosition(), e.getTable(), reshapeInner(e.get(0)));
	}

	public Expression reshapeXdef(Xsymbol e) {
		return ExpressionCommons.newXsymbol(e.getSourcePosition(), e.getTable(), reshapeInner(e.get(0)));
	}

	public Expression reshapeXmatch(Xmatch e) {
		return e; // immutable
	}

	public Expression reshapeXis(Xis e) {
		return ExpressionCommons.newXis(e.getSourcePosition(), e.getTable(), reshapeInner(e.get(0)), e.is);
	}

	public Expression reshapeXexists(Xexists e) {
		return ExpressionCommons.newXexists(e.getSourcePosition(), e.getTable(), e.getSymbol());
	}

	public Expression reshapeXindent(Xindent e) {
		return e; // immutable
	}

	public Expression reshapeXif(Xif e) {
		return e; // immutable
	}

	public Expression reshapeXon(Xon e) {
		return ExpressionCommons.newXon(e.getSourcePosition(), e.isPositive(), e.getFlagName(), reshapeInner(e.get(0)));
	}

	public Expression reshapeUndefined(Expression e) {
		Verbose.println("TODO: implement reshape in " + this.getClass());
		return e;
	}

}
