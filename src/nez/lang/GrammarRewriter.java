package nez.lang;

import nez.lang.expr.Cany;
import nez.lang.expr.Cbyte;
import nez.lang.expr.Cmulti;
import nez.lang.expr.Cset;
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

public class GrammarRewriter extends GrammarTransducer {

	@Override
	public Production visitProduction(Production p) {
		return p;
	}

	@Override
	public Expression visitPempty(Pempty e, Object a) {
		return e;
	}

	@Override
	public Expression visitPfail(Pfail e, Object a) {
		return e;
	}

	@Override
	public Expression visitCbyte(Cbyte e, Object a) {
		return e;
	}

	@Override
	public Expression visitCset(Cset e, Object a) {
		return e;
	}

	@Override
	public Expression visitCany(Cany e, Object a) {
		return e;
	}

	@Override
	public Expression visitCmulti(Cmulti e, Object a) {
		return e;
	}

	@Override
	public Expression visitNonTerminal(NonTerminal e, Object a) {
		return e;
	}

	@Override
	public Expression visitPsequence(Psequence e, Object a) {
		Expression first = e.getFirst();
		push(first);
		first = (Expression) first.visit(this, a);
		Expression next = e.getNext();
		push(next);
		next = (Expression) next.visit(this, a);
		pop(e.getNext());
		pop(e.getFirst());
		if (first instanceof Pempty) {
			return next;
		}
		if (first instanceof Pfail || next instanceof Pempty) {
			return first;
		}
		e.first = first;
		e.next = next;
		return e;
	}

	@Override
	public Expression visitPchoice(Pchoice e, Object a) {
		int i = 0;
		for (i = 0; i < e.size(); i++) {
			e.set(i, visitInner(e.get(i)));
		}
		return e;
	}

	@Override
	public Expression visitPoption(Poption e, Object a) {
		e.set(0, this.visitInner(e.get(0)));
		return e;
	}

	@Override
	public Expression visitPzero(Pzero e, Object a) {
		e.set(0, this.visitInner(e.get(0)));
		return e;
	}

	@Override
	public Expression visitPone(Pone e, Object a) {
		e.set(0, this.visitInner(e.get(0)));
		return e;
	}

	@Override
	public Expression visitPand(Pand e, Object a) {
		e.set(0, this.visitInner(e.get(0)));
		return e;
	}

	@Override
	public Expression visitPnot(Pnot e, Object a) {
		e.set(0, this.visitInner(e.get(0)));
		return e;
	}

	@Override
	public Expression visitTdetree(Tdetree e, Object a) {
		e.set(0, this.visitInner(e.get(0)));
		return e;
	}

	@Override
	public Expression visitTnew(Tnew e, Object a) {
		return e;
	}

	@Override
	public Expression visitTlink(Tlink e, Object a) {
		e.set(0, this.visitInner(e.get(0)));
		return e;
	}

	@Override
	public Expression visitTtag(Ttag e, Object a) {
		return e;
	}

	@Override
	public Expression visitTreplace(Treplace e, Object a) {
		return e;
	}

	@Override
	public Expression visitTcapture(Tcapture e, Object a) {
		return e;
	}

	@Override
	public Expression visitXblock(Xblock e, Object a) {
		e.set(0, this.visitInner(e.get(0)));
		return e;
	}

	@Override
	public Expression visitXlocal(Xlocal e, Object a) {
		e.set(0, this.visitInner(e.get(0)));
		return e;
	}

	@Override
	public Expression visitXdef(Xsymbol e, Object a) {
		e.set(0, this.visitInner(e.get(0)));
		return e;
	}

	@Override
	public Expression visitXmatch(Xmatch e, Object a) {
		return e;
	}

	@Override
	public Expression visitXis(Xis e, Object a) {
		e.set(0, this.visitInner(e.get(0)));
		return e;
	}

	@Override
	public Expression visitXexists(Xexists e, Object a) {
		return e;
	}

	@Override
	public Expression visitXindent(Xindent e, Object a) {
		return e;
	}

	@Override
	public Expression visitXif(Xif e, Object a) {
		return e;
	}

	@Override
	public Expression visitXon(Xon e, Object a) {
		e.set(0, this.visitInner(e.get(0)));
		return e;
	}

}