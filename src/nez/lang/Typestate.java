package nez.lang;

import nez.lang.Nez.Byte;
import nez.util.Verbose;

public enum Typestate {
	Unit, Tree, TreeMutation, Immutation, Undecided;
	// // public final static Integer Undecided = -1;
	// public final static Integer Unit = 0;
	// public final static Integer Tree = 1;
	// public final static Integer TreeMutation = 2;

	public static interface TypestateAnalyzer {
		public Typestate inferTypestate(Production p);

		public Typestate inferTypestate(Expression e);

		public boolean isUnit(Production p);

		public boolean isUnit(Expression e);

		public boolean isTree(Production p);

		public boolean isTree(Expression e);
	}

	public static final TypestateAnalyzer newAnalyzer() {
		return new Analyzer();
	}

	final static class Analyzer extends Expression.Visitor implements TypestateAnalyzer {

		@Override
		public boolean isUnit(Production p) {
			return this.inferTypestate(p) == Unit;
		}

		@Override
		public boolean isUnit(Expression e) {
			return this.inferTypestate(e) == Unit;
		}

		@Override
		public boolean isTree(Production p) {
			return this.inferTypestate(p) == Tree;
		}

		@Override
		public boolean isTree(Expression e) {
			return this.inferTypestate(e) == Tree;
		}

		@Override
		public Typestate inferTypestate(Expression e) {
			return (Typestate) e.visit(this, null);
		}

		@Override
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
		public Object visitMultiByte(Nez.MultiByte e, Object a) {
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

		@Override
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
		public Object visitBeginTree(Nez.BeginTree e, Object a) {
			return Typestate.Tree;
		}

		@Override
		public Object visitFoldTree(Nez.FoldTree e, Object a) {
			return Typestate.TreeMutation;
		}

		@Override
		public Object visitLinkTree(Nez.LinkTree e, Object a) {
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
		public Object visitEndTree(Nez.EndTree e, Object a) {
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
		public final Object visitScan(Nez.Scan e, Object a) {
			return this.inferTypestate(e.get(0));
		}

		@Override
		public final Object visitRepeat(Nez.Repeat e, Object a) {
			Typestate ts = this.inferTypestate(e.get(0));
			if (ts == Typestate.Tree) {
				return Typestate.TreeMutation;
			}
			return ts;
		}

		@Override
		public final Object visitIf(Nez.IfCondition e, Object a) {
			return Typestate.Unit;
		}

		@Override
		public final Object visitOn(Nez.OnCondition e, Object a) {
			return this.inferTypestate(e.get(0));
		}

		@Override
		public Object visitLabel(Nez.Label e, Object a) {
			return Typestate.Unit;
		}

	}
}
