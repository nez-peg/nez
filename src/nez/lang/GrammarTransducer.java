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
import nez.util.UList;
import nez.util.Verbose;

public class GrammarTransducer extends Expression.Visitor {

	public Expression visitInner(Expression inner) {
		push(inner);
		Expression inner2 = (Expression) inner.visit(this, null);
		pop(inner);
		return inner2;
	}

	protected void push(Expression inner) {
	}

	protected void pop(Expression inner) {
	}

	public Production visitProduction(Production p) {
		p.setExpression(this.visitInner(p.getExpression()));
		return p;
	}

	@Override
	public Expression visitPempty(Pempty e, Object a) {
		return e; // immutable
	}

	@Override
	public Expression visitPfail(Pfail e, Object a) {
		return e; // immutable
	}

	@Override
	public Expression visitCbyte(Cbyte e, Object a) {
		return e; // immutable
	}

	@Override
	public Expression visitCset(Cset e, Object a) {
		return e; // immutable
	}

	@Override
	public Expression visitCany(Cany e, Object a) {
		return e; // immutable
	}

	@Override
	public Expression visitCmulti(Cmulti e, Object a) {
		return e; // immutable
	}

	@Override
	public Expression visitNonTerminal(NonTerminal e, Object a) {
		e.newNonTerminal(e.getLocalName());
		return e;
	}

	@Override
	public Expression visitPsequence(Psequence e, Object a) {
		Expression first = e.getFirst();
		push(first);
		first = (Expression) first.visit(this, null);
		Expression next = e.getNext();
		push(next);
		next = (Expression) next.visit(this, null);
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

	@Override
	public Expression visitPchoice(Pchoice e, Object a) {
		UList<Expression> l = ExpressionCommons.newList(e.size());
		for (Expression sub : e) {
			ExpressionCommons.addChoice(l, this.visitInner(sub));
		}
		return e.newChoice(l);
	}

	@Override
	public Expression visitPoption(Poption e, Object a) {
		return ExpressionCommons.newPoption(e.getSourcePosition(), visitInner(e.get(0)));
	}

	@Override
	public Expression visitPzero(Pzero e, Object a) {
		return ExpressionCommons.newPzero(e.getSourcePosition(), visitInner(e.get(0)));
	}

	@Override
	public Expression visitPone(Pone e, Object a) {
		return ExpressionCommons.newPone(e.getSourcePosition(), visitInner(e.get(0)));
	}

	@Override
	public Expression visitPand(Pand e, Object a) {
		return ExpressionCommons.newPand(e.getSourcePosition(), visitInner(e.get(0)));
	}

	@Override
	public Expression visitPnot(Pnot e, Object a) {
		return ExpressionCommons.newPnot(e.getSourcePosition(), visitInner(e.get(0)));
	}

	@Override
	public Expression visitTnew(Tnew e, Object a) {
		return ExpressionCommons.newTnew(e.getSourcePosition(), e.shift);
	}

	@Override
	public Expression visitTlfold(Tlfold e, Object a) {
		return ExpressionCommons.newTlfold(e.getSourcePosition(), e.getLabel(), e.shift);
	}

	@Override
	public Expression visitTlink(Tlink e, Object a) {
		return ExpressionCommons.newTlink(e.getSourcePosition(), e.getLabel(), visitInner(e.get(0)));
	}

	@Override
	public Expression visitTtag(Ttag e, Object a) {
		return e; // immutable
	}

	@Override
	public Expression visitTreplace(Treplace e, Object a) {
		return e; // immutable
	}

	@Override
	public Expression visitTcapture(Tcapture e, Object a) {
		return ExpressionCommons.newTcapture(e.getSourcePosition(), e.shift);
	}

	@Override
	public Expression visitTdetree(Tdetree e, Object a) {
		return ExpressionCommons.newTdetree(e.getSourcePosition(), visitInner(e.get(0)));
	}

	@Override
	public Expression visitXblock(Xblock e, Object a) {
		return ExpressionCommons.newXblock(e.getSourcePosition(), visitInner(e.get(0)));
	}

	@Override
	public Expression visitXlocal(Xlocal e, Object a) {
		return ExpressionCommons.newXlocal(e.getSourcePosition(), e.getTable(), visitInner(e.get(0)));
	}

	@Override
	public Expression visitXdef(Xsymbol e, Object a) {
		return ExpressionCommons.newXsymbol(e.getSourcePosition(), e.getTable(), visitInner(e.get(0)));
	}

	@Override
	public Expression visitXmatch(Xmatch e, Object a) {
		return e; // immutable
	}

	@Override
	public Expression visitXis(Xis e, Object a) {
		return ExpressionCommons.newXis(e.getSourcePosition(), e.getTable(), visitInner(e.get(0)), e.is);
	}

	@Override
	public Expression visitXexists(Xexists e, Object a) {
		return ExpressionCommons.newXexists(e.getSourcePosition(), e.getTable(), e.getSymbol());
	}

	@Override
	public Expression visitXindent(Xindent e, Object a) {
		return e; // immutable
	}

	@Override
	public Expression visitXif(Xif e, Object a) {
		return e; // immutable
	}

	@Override
	public Expression visitXon(Xon e, Object a) {
		return ExpressionCommons.newXon(e.getSourcePosition(), e.isPositive(), e.getFlagName(), visitInner(e.get(0)));
	}

	@Override
	public Expression visitUndefined(Expression e, Object a) {
		Verbose.println("TODO: implement visit in " + this.getClass());
		return e;
	}

}
