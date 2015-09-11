package nez.lang;

import nez.lang.expr.Cany;
import nez.lang.expr.Cbyte;
import nez.lang.expr.Cmulti;
import nez.lang.expr.Cset;
import nez.lang.expr.NonTerminal;
import nez.lang.expr.Pchoice;
import nez.lang.expr.Pempty;
import nez.lang.expr.Pfail;
import nez.lang.expr.Psequence;
import nez.lang.expr.Tcapture;
import nez.lang.expr.Tlink;
import nez.lang.expr.Tnew;
import nez.lang.expr.Treplace;
import nez.lang.expr.Ttag;
import nez.lang.expr.Pand;
import nez.lang.expr.Pdetree;
import nez.lang.expr.Pnot;
import nez.lang.expr.Pone;
import nez.lang.expr.Poption;
import nez.lang.expr.Pzero;
import nez.lang.expr.Xblock;
import nez.lang.expr.Xdef;
import nez.lang.expr.Xexists;
import nez.lang.expr.Xif;
import nez.lang.expr.Xindent;
import nez.lang.expr.Xis;
import nez.lang.expr.Xlocal;
import nez.lang.expr.Xmatch;
import nez.lang.expr.Xon;

public class GrammarRewriter extends GrammarTransducer {

	@Override
	public Production reshapeProduction(Production p) {
		return p;
	}

	@Override
	public Expression reshapePempty(Pempty e) {
		return e;
	}

	@Override
	public Expression reshapePfail(Pfail e) {
		return e;
	}

	@Override
	public Expression reshapeCbyte(Cbyte e) {
		return e;
	}

	@Override
	public Expression reshapeCset(Cset e) {
		return e;
	}

	@Override
	public Expression reshapeCany(Cany e) {
		return e;
	}

	@Override
	public Expression reshapeCmulti(Cmulti e) {
		return e;
	}

	@Override
	public Expression reshapeNonTerminal(NonTerminal e) {
		return e;
	}

	@Override
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
		if (first instanceof Pfail || next instanceof Pempty) {
			return first;
		}
		e.first = first;
		e.next = next;
		return e;
	}

	@Override
	public Expression reshapePchoice(Pchoice e) {
		int i = 0;
		for (i = 0; i < e.size(); i++) {
			e.set(i, reshapeInner(e.get(i)));
		}
		return e;
	}

	@Override
	public Expression reshapePoption(Poption e) {
		e.set(0, this.reshapeInner(e.get(0)));
		return e;
	}

	@Override
	public Expression reshapePzero(Pzero e) {
		e.set(0, this.reshapeInner(e.get(0)));
		return e;
	}

	@Override
	public Expression reshapePone(Pone e) {
		e.set(0, this.reshapeInner(e.get(0)));
		return e;
	}

	@Override
	public Expression reshapePand(Pand e) {
		e.set(0, this.reshapeInner(e.get(0)));
		return e;
	}

	@Override
	public Expression reshapePnot(Pnot e) {
		e.set(0, this.reshapeInner(e.get(0)));
		return e;
	}

	@Override
	public Expression reshapePdetree(Pdetree e) {
		e.set(0, this.reshapeInner(e.get(0)));
		return e;
	}

	@Override
	public Expression reshapeTnew(Tnew e) {
		return e;
	}

	@Override
	public Expression reshapeTlink(Tlink e) {
		e.set(0, this.reshapeInner(e.get(0)));
		return e;
	}

	@Override
	public Expression reshapeTtag(Ttag e) {
		return e;
	}

	@Override
	public Expression reshapeTreplace(Treplace e) {
		return e;
	}

	@Override
	public Expression reshapeTcapture(Tcapture e) {
		return e;
	}

	@Override
	public Expression reshapeXblock(Xblock e) {
		e.set(0, this.reshapeInner(e.get(0)));
		return e;
	}

	@Override
	public Expression reshapeXlocal(Xlocal e) {
		e.set(0, this.reshapeInner(e.get(0)));
		return e;
	}

	@Override
	public Expression reshapeXdef(Xdef e) {
		e.set(0, this.reshapeInner(e.get(0)));
		return e;
	}

	@Override
	public Expression reshapeXmatch(Xmatch e) {
		return e;
	}

	@Override
	public Expression reshapeXis(Xis e) {
		return e;
	}

	@Override
	public Expression reshapeXexists(Xexists e) {
		return e;
	}

	@Override
	public Expression reshapeXindent(Xindent e) {
		return e;
	}

	@Override
	public Expression reshapeXif(Xif e) {
		return e;
	}

	@Override
	public Expression reshapeXon(Xon e) {
		e.set(0, this.reshapeInner(e.get(0)));
		return e;
	}

}