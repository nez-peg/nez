//package nez.lang;
//
//import nez.lang.expr.Cany;
//import nez.lang.expr.Cbyte;
//import nez.lang.expr.Cmulti;
//import nez.lang.expr.Cset;
//import nez.lang.expr.NonTerminal;
//import nez.lang.expr.Pand;
//import nez.lang.expr.Pchoice;
//import nez.lang.expr.Pempty;
//import nez.lang.expr.Pfail;
//import nez.lang.expr.Pnot;
//import nez.lang.expr.Pone;
//import nez.lang.expr.Poption;
//import nez.lang.expr.Psequence;
//import nez.lang.expr.Pzero;
//import nez.lang.expr.Tcapture;
//import nez.lang.expr.Tdetree;
//import nez.lang.expr.Tlfold;
//import nez.lang.expr.Tlink;
//import nez.lang.expr.Tnew;
//import nez.lang.expr.Treplace;
//import nez.lang.expr.Ttag;
//import nez.lang.expr.Xblock;
//import nez.lang.expr.Xsymbol;
//import nez.lang.expr.Xdefindent;
//import nez.lang.expr.Xexists;
//import nez.lang.expr.Xif;
//import nez.lang.expr.Xindent;
//import nez.lang.expr.Xis;
//import nez.lang.expr.Xlocal;
//import nez.lang.expr.Xmatch;
//import nez.lang.expr.Xon;
//
//public abstract class GrammarVisitor extends GrammarTransducer {
//	public GrammarVisitor() {
//	}
//
//	public void visitExpression(Expression e) {
//		e.visit(this);
//	}
//
//	public void visitUndefined(Expression p) {
//
//	}
//
//	public abstract void visitPempty(Expression p);
//
//	public abstract void visitPfail(Expression p);
//
//	public abstract void visitCany(Cany p);
//
//	public abstract void visitCbyte(Cbyte p);
//
//	public abstract void visitCset(Cset p);
//
//	public abstract void visitCmulti(Cmulti p);
//
//	public abstract void visitPoption(Poption p);
//
//	public abstract void visitPzero(Pzero p);
//
//	public abstract void visitPone(Pone p);
//
//	public abstract void visitPand(Pand p);
//
//	public abstract void visitPnot(Pnot p);
//
//	public abstract void visitPsequence(Psequence p);
//
//	public abstract void visitPchoice(Pchoice p);
//
//	public abstract void visitNonTerminal(NonTerminal p);
//
//	// AST Construction
//	public abstract void visitTlink(Tlink p);
//
//	public abstract void visitTnew(Tnew p);
//
//	public abstract void visitTlfold(Tlfold p);
//
//	public abstract void visitTcapture(Tcapture p);
//
//	public abstract void visitTtag(Ttag p);
//
//	public abstract void visitTreplace(Treplace p);
//
//	public abstract void visitTdetree(Tdetree p);
//
//	// Symbol Tables
//	public abstract void visitXblock(Xblock p);
//
//	public abstract void visitXlocal(Xlocal p);
//
//	public abstract void visitXdef(Xsymbol p);
//
//	public abstract void visitXexists(Xexists p);
//
//	public abstract void visitXmatch(Xmatch p);
//
//	public abstract void visitXis(Xis p);
//
//	public abstract void visitXif(Xif p);
//
//	public abstract void visitXon(Xon p);
//
//	public abstract void visitXdefindent(Xdefindent p);
//
//	public abstract void visitXindent(Xindent p);
//
//	@Override
//	public final Expression visitCany(Cany p) {
//		this.visitCany(p);
//		return null;
//	}
//
//	@Override
//	public final Expression visitCbyte(Cbyte p) {
//		this.visitCbyte(p);
//		return null;
//	}
//
//	@Override
//	public final Expression visitCset(Cset p) {
//		this.visitCset(p);
//		return null;
//	}
//
//	@Override
//	public final Expression visitCmulti(Cmulti p) {
//		this.visitCmulti(p);
//		return null;
//	}
//
//	@Override
//	public final Expression visitPfail(Pfail p) {
//		this.visitPfail(p);
//		return null;
//	}
//
//	@Override
//	public final Expression visitPoption(Poption p) {
//		this.visitPoption(p);
//		return null;
//	}
//
//	@Override
//	public final Expression visitPzero(Pzero p) {
//		this.visitPzero(p);
//		return null;
//	}
//
//	@Override
//	public final Expression visitPone(Pone p) {
//		this.visitPone(p);
//		return null;
//	}
//
//	@Override
//	public final Expression visitPand(Pand p) {
//		this.visitPand(p);
//		return null;
//	}
//
//	@Override
//	public final Expression visitPnot(Pnot p) {
//		this.visitPnot(p);
//		return null;
//	}
//
//	@Override
//	public final Expression visitPsequence(Psequence p) {
//		this.visitPsequence(p);
//		return null;
//	}
//
//	@Override
//	public final Expression visitPchoice(Pchoice p) {
//		this.visitPchoice(p);
//		return null;
//	}
//
//	@Override
//	public final Expression visitNonTerminal(NonTerminal p) {
//		this.visitNonTerminal(p);
//		return null;
//	}
//
//	// AST Construction
//
//	@Override
//	public final Expression visitTdetree(Tdetree p) {
//		this.visitTdetree(p);
//		return null;
//	}
//
//	@Override
//	public final Expression visitTlink(Tlink p) {
//		this.visitTlink(p);
//		return null;
//	}
//
//	@Override
//	public final Expression visitTnew(Tnew p) {
//		this.visitTnew(p);
//		return null;
//	}
//
//	@Override
//	public final Expression visitTlfold(Tlfold p) {
//		this.visitTlfold(p);
//		return null;
//	}
//
//	@Override
//	public final Expression visitTcapture(Tcapture p) {
//		this.visitTcapture(p);
//		return null;
//	}
//
//	@Override
//	public final Expression visitTtag(Ttag p) {
//		this.visitTtag(p);
//		return null;
//	}
//
//	@Override
//	public final Expression visitTreplace(Treplace p) {
//		this.visitTreplace(p);
//		return null;
//	}
//
//	@Override
//	public final Expression visitXblock(Xblock p) {
//		this.visitXblock(p);
//		return null;
//	}
//
//	@Override
//	public final Expression visitXlocal(Xlocal p) {
//		this.visitXlocal(p);
//		return null;
//	}
//
//	@Override
//	public final Expression visitXdef(Xsymbol p) {
//		this.visitXdef(p);
//		return null;
//	}
//
//	@Override
//	public final Expression visitXexists(Xexists p) {
//		this.visitXexists(p);
//		return null;
//	}
//
//	@Override
//	public final Expression visitXmatch(Xmatch p) {
//		this.visitXmatch(p);
//		return null;
//	}
//
//	@Override
//	public final Expression visitXis(Xis p) {
//		this.visitXis(p);
//		return null;
//	}
//
//	// @Override
//	// public final Expression visitXdefindent(Xdefindent p) {
//	// this.visitXdefindent(p);
//	// return null;
//	// }
//
//	@Override
//	public final Expression visitXindent(Xindent p) {
//		this.visitXindent(p);
//		return null;
//	}
//
//	@Override
//	public final Expression visitPempty(Pempty p) {
//		this.visitPempty(p);
//		return null;
//	}
//
//	@Override
//	public final Expression visitXon(Xon p) {
//		this.visitXon(p);
//		return null;
//	}
//
//	@Override
//	public final Expression visitXif(Xif p) {
//		this.visitXif(p);
//		return null;
//	}
//
// }
