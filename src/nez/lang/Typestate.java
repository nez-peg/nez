package nez.lang;

import nez.lang.Nez.Byte;
import nez.lang.expr.NonTerminal;
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
		public Object visitEmpty(Nez.Empty e, Object a) {
			return Typestate.Unit;
		}

		@Override
		public Object visitFail(Nez.Fail e, Object a) {
			return Typestate.Unit;
		}

		@Override
		public Object visitByte(Byte e, Object a) {
			return Typestate.Unit;
		}

		@Override
		public Object visitByteSet(Nez.ByteSet e, Object a) {
			return Typestate.Unit;
		}

		@Override
		public Object visitAny(Nez.Any e, Object a) {
			return Typestate.Unit;
		}

		@Override
		public Object visitString(Nez.String e, Object a) {
			return Typestate.Unit;
		}

		@Override
		public Object visitPair(Nez.Pair e, Object a) {
			for (Expression s : e) {
				Typestate ts = inferTypestate(s);
				if (ts == Typestate.Tree || ts == Typestate.TreeMutation) {
					return ts;
				}
			}
			return Typestate.Unit;
		}

		public Object visitSequence(Nez.Sequence e, Object a) {
			for (Expression s : e) {
				Typestate ts = inferTypestate(s);
				if (ts == Typestate.Tree || ts == Typestate.TreeMutation) {
					return ts;
				}
			}
			return Typestate.Unit;
		}

		@Override
		public Object visitChoice(Nez.Choice e, Object a) {
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
		public Object visitOption(Nez.Option e, Object a) {
			Typestate ts = this.inferTypestate(e.get(0));
			if (ts == Typestate.Tree) {
				return Typestate.TreeMutation;
			}
			return ts;
		}

		@Override
		public Object visitZeroMore(Nez.ZeroMore e, Object a) {
			Typestate ts = this.inferTypestate(e.get(0));
			if (ts == Typestate.Tree) {
				return Typestate.TreeMutation;
			}
			return ts;
		}

		@Override
		public Object visitOneMore(Nez.OneMore e, Object a) {
			Typestate ts = this.inferTypestate(e.get(0));
			if (ts == Typestate.Tree) {
				return Typestate.TreeMutation;
			}
			return ts;
		}

		@Override
		public Object visitAnd(Nez.And e, Object a) {
			Typestate ts = this.inferTypestate(e.get(0));
			if (ts == Typestate.Tree) { // typeCheck needs to report error
				return Typestate.Unit;
			}
			return ts;
		}

		@Override
		public Object visitNot(Nez.Not e, Object a) {
			return Typestate.Unit;
		}

		@Override
		public Object visitPreNew(Nez.PreNew e, Object a) {
			return Typestate.Tree;
		}

		@Override
		public Object visitLeftFold(Nez.LeftFold e, Object a) {
			return Typestate.TreeMutation;
		}

		@Override
		public Object visitLink(Nez.Link e, Object a) {
			return Typestate.TreeMutation;
		}

		@Override
		public Object visitTag(Nez.Tag e, Object a) {
			return Typestate.TreeMutation;
		}

		@Override
		public Object visitReplace(Nez.Replace e, Object a) {
			return Typestate.TreeMutation;
		}

		@Override
		public Object visitNew(Nez.New e, Object a) {
			return Typestate.TreeMutation;
		}

		@Override
		public Object visitDetree(Nez.Detree e, Object a) {
			return Typestate.Unit;
		}

		@Override
		public final Object visitBlockScope(Nez.BlockScope e, Object a) {
			return this.inferTypestate(e.get(0));
		}

		@Override
		public final Object visitLocalScope(Nez.LocalScope e, Object a) {
			return this.inferTypestate(e.get(0));
		}

		@Override
		public final Object visitSymbolAction(Nez.SymbolAction e, Object a) {
			return this.inferTypestate(e.get(0));
		}

		@Override
		public final Object visitSymbolMatch(Nez.SymbolMatch e, Object a) {
			return Typestate.Unit;
		}

		@Override
		public final Object visitSymbolPredicate(Nez.SymbolPredicate e, Object a) {
			return this.inferTypestate(e.get(0));
		}

		@Override
		public final Object visitSymbolExists(Nez.SymbolExists e, Object a) {
			return Typestate.Unit;
		}

		@Override
		public final Object visitIf(Nez.If e, Object a) {
			return Typestate.Unit;
		}

		@Override
		public final Object visitOn(Nez.On e, Object a) {
			return this.inferTypestate(e.get(0));
		}

		// @Override
		// public final Object visitSetCount(Nez.SetCount e, Object a) {
		// return this.inferTypestate(e.get(0));
		// }
		//
		// @Override
		// public final Object visitCount(Nez.Count e, Object a) {
		// return this.inferTypestate(e.get(0));
		// }

	}
}
