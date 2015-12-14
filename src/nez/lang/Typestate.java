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
import nez.util.Verbose;

public enum Typestate {
	Unit, Tree, TreeMutation, Undecided;
	// // public final static Integer Undecided = -1;
	// public final static Integer Unit = 0;
	// public final static Integer Tree = 1;
	// public final static Integer TreeMutation = 2;

	public final static class Analyzer extends Expression.Visitor {

		public Typestate inferTypestate(Expression e) {
			return (Typestate) e.visit(this, null);
		}

		public Typestate inferTypestate(Production p) {
			String uname = p.getUniqueName();
			Object v = this.lookup(uname);
			if (v == null) {
				this.visited(uname);
				v = this.inferTypestate(p.getExpression());
				if (Typestate.Undecided != v) {
					this.memo(uname, v);
				}
				return (Typestate) v;
			}
			return (v instanceof Typestate) ? (Typestate) v : Typestate.Undecided;
		}

		@Override
		public Object visitNonTerminal(NonTerminal e, Object a) {
			Production p = e.getProduction();
			if (p == null) {
				if (!e.isTerminal()) {
					Verbose.debug("** unresolved name: " + e.getLocalName());
				}
				return Typestate.Unit;
			}
			return this.inferTypestate(p);
		}

		@Override
		public Object visitPempty(Pempty e, Object a) {
			return Typestate.Unit;
		}

		@Override
		public Object visitPfail(Pfail e, Object a) {
			return Typestate.Unit;
		}

		@Override
		public Object visitCbyte(Cbyte e, Object a) {
			return Typestate.Unit;
		}

		@Override
		public Object visitCset(Cset e, Object a) {
			return Typestate.Unit;
		}

		@Override
		public Object visitCany(Cany e, Object a) {
			return Typestate.Unit;
		}

		@Override
		public Object visitCmulti(Cmulti e, Object a) {
			return Typestate.Unit;
		}

		@Override
		public Object visitPsequence(Psequence e, Object a) {
			for (Expression s : e) {
				Typestate ts = inferTypestate(s);
				if (ts == Typestate.Tree || ts == Typestate.TreeMutation) {
					return ts;
				}
			}
			return Typestate.Unit;
		}

		@Override
		public Object visitPchoice(Pchoice e, Object a) {
			Object t = Typestate.Unit;
			for (Expression s : e) {
				t = this.inferTypestate(s);
				if (t == Typestate.Tree || t == Typestate.TreeMutation) {
					return t;
				}
			}
			return t;
		}

		@Override
		public Object visitPoption(Poption e, Object a) {
			Typestate ts = this.inferTypestate(e.get(0));
			if (ts == Typestate.Tree) {
				return Typestate.TreeMutation;
			}
			return ts;
		}

		@Override
		public Object visitPzero(Pzero e, Object a) {
			Typestate ts = this.inferTypestate(e.get(0));
			if (ts == Typestate.Tree) {
				return Typestate.TreeMutation;
			}
			return ts;
		}

		@Override
		public Object visitPone(Pone e, Object a) {
			Typestate ts = this.inferTypestate(e.get(0));
			if (ts == Typestate.Tree) {
				return Typestate.TreeMutation;
			}
			return ts;
		}

		@Override
		public Object visitPand(Pand e, Object a) {
			Typestate ts = this.inferTypestate(e.get(0));
			if (ts == Typestate.Tree) { // typeCheck needs to report error
				return Typestate.Unit;
			}
			return ts;
		}

		@Override
		public Object visitPnot(Pnot e, Object a) {
			return Typestate.Unit;
		}

		@Override
		public Object visitTnew(Tnew e, Object a) {
			return Typestate.Tree;
		}

		@Override
		public Object visitTlfold(Tlfold e, Object a) {
			return Typestate.TreeMutation;
		}

		@Override
		public Object visitTlink(Tlink e, Object a) {
			return Typestate.TreeMutation;
		}

		@Override
		public Object visitTtag(Ttag e, Object a) {
			return Typestate.TreeMutation;
		}

		@Override
		public Object visitTreplace(Treplace e, Object a) {
			return Typestate.TreeMutation;
		}

		@Override
		public Object visitTcapture(Tcapture e, Object a) {
			return Typestate.TreeMutation;
		}

		@Override
		public Object visitTdetree(Tdetree e, Object a) {
			return Typestate.Unit;
		}

		@Override
		public final Object visitXblock(Xblock e, Object a) {
			return this.inferTypestate(e.get(0));
		}

		@Override
		public final Object visitXlocal(Xlocal e, Object a) {
			return this.inferTypestate(e.get(0));
		}

		@Override
		public final Object visitXdef(Xsymbol e, Object a) {
			return this.inferTypestate(e.get(0));
		}

		@Override
		public final Object visitXmatch(Xmatch e, Object a) {
			return Typestate.Unit;
		}

		@Override
		public final Object visitXis(Xis e, Object a) {
			return this.inferTypestate(e.get(0));
		}

		@Override
		public final Object visitXexists(Xexists e, Object a) {
			return Typestate.Unit;
		}

		@Override
		public final Object visitXindent(Xindent e, Object a) {
			return Typestate.Unit;
		}

		@Override
		public final Object visitXif(Xif e, Object a) {
			return Typestate.Unit;
		}

		@Override
		public final Object visitXon(Xon e, Object a) {
			return this.inferTypestate(e.get(0));
		}

	}
}
