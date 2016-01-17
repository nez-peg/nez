package nez.lang;

import nez.lang.Nez.Byte;

public enum Symbolstate {
	Unchanged, Mutated, Undecided;

	public static interface StateAnalyzer {
		public boolean isStateful(Expression e);
	}

	public static final StateAnalyzer newAnalyzer() {
		return new Analyzer();
	}

	final static class Analyzer extends Expression.Visitor implements StateAnalyzer {

		@Override
		public boolean isStateful(Expression e) {
			Symbolstate s = (Symbolstate) e.visit(this, null);
			return (s != Symbolstate.Unchanged);
		}

		public Symbolstate inferSymbolstate(Production p) {
			String uname = p.getUniqueName();
			Object v = this.lookup(uname);
			if (v == null) {
				this.visited(uname);
				v = p.getExpression().visit(this, null);
				if (Symbolstate.Undecided != v) {
					this.memo(uname, v);
				}
				return (Symbolstate) v;
			}
			return (v instanceof Symbolstate) ? (Symbolstate) v : Symbolstate.Undecided;
		}

		private Symbolstate visitExpression(Expression e) {
			Symbolstate result = Symbolstate.Unchanged;
			for (Expression sub : e) {
				Symbolstate ts = (Symbolstate) sub.visit(this, null);
				if (ts == Symbolstate.Mutated) {
					return ts;
				}
				if (ts == Symbolstate.Undecided) {
					result = ts;
				}
			}
			return result;
		}

		@Override
		public Object visitNonTerminal(NonTerminal e, Object a) {
			Production p = e.getProduction();
			return this.inferSymbolstate(p);
		}

		@Override
		public Object visitEmpty(Nez.Empty e, Object a) {
			return Symbolstate.Unchanged;
		}

		@Override
		public Object visitFail(Nez.Fail e, Object a) {
			return Symbolstate.Unchanged;
		}

		@Override
		public Object visitByte(Byte e, Object a) {
			return Symbolstate.Unchanged;
		}

		@Override
		public Object visitByteSet(Nez.ByteSet e, Object a) {
			return Symbolstate.Unchanged;
		}

		@Override
		public Object visitAny(Nez.Any e, Object a) {
			return Symbolstate.Unchanged;
		}

		@Override
		public Object visitMultiByte(Nez.MultiByte e, Object a) {
			return Symbolstate.Unchanged;
		}

		@Override
		public Object visitPair(Nez.Pair e, Object a) {
			return visitExpression(e);
		}

		@Override
		public Object visitSequence(Nez.Sequence e, Object a) {
			return visitExpression(e);
		}

		@Override
		public Object visitChoice(Nez.Choice e, Object a) {
			return visitExpression(e);
		}

		@Override
		public Object visitOption(Nez.Option e, Object a) {
			return visitExpression(e);
		}

		@Override
		public Object visitZeroMore(Nez.ZeroMore e, Object a) {
			return visitExpression(e);
		}

		@Override
		public Object visitOneMore(Nez.OneMore e, Object a) {
			return visitExpression(e);
		}

		@Override
		public Object visitAnd(Nez.And e, Object a) {
			return visitExpression(e);
		}

		@Override
		public Object visitNot(Nez.Not e, Object a) {
			return Symbolstate.Unchanged;
		}

		@Override
		public Object visitBeginTree(Nez.BeginTree e, Object a) {
			return Symbolstate.Unchanged;
		}

		@Override
		public Object visitFoldTree(Nez.FoldTree e, Object a) {
			return Symbolstate.Unchanged;
		}

		@Override
		public Object visitLinkTree(Nez.LinkTree e, Object a) {
			return visitExpression(e);
		}

		@Override
		public Object visitTag(Nez.Tag e, Object a) {
			return Symbolstate.Unchanged;
		}

		@Override
		public Object visitReplace(Nez.Replace e, Object a) {
			return Symbolstate.Unchanged;
		}

		@Override
		public Object visitEndTree(Nez.EndTree e, Object a) {
			return Symbolstate.Unchanged;
		}

		@Override
		public Object visitDetree(Nez.Detree e, Object a) {
			return visitExpression(e);
		}

		@Override
		public final Object visitBlockScope(Nez.BlockScope e, Object a) {
			return Symbolstate.Unchanged;
		}

		@Override
		public final Object visitLocalScope(Nez.LocalScope e, Object a) {
			return Symbolstate.Unchanged;
		}

		@Override
		public final Object visitSymbolAction(Nez.SymbolAction e, Object a) {
			return Symbolstate.Mutated;
		}

		@Override
		public final Object visitSymbolMatch(Nez.SymbolMatch e, Object a) {
			return Symbolstate.Unchanged;
		}

		@Override
		public final Object visitSymbolPredicate(Nez.SymbolPredicate e, Object a) {
			return visitExpression(e);
		}

		@Override
		public final Object visitSymbolExists(Nez.SymbolExists e, Object a) {
			return Symbolstate.Unchanged;
		}

		@Override
		public final Object visitScanf(Nez.Scanf e, Object a) {
			return visitExpression(e);
		}

		@Override
		public final Object visitRepeat(Nez.Repeat e, Object a) {
			return visitExpression(e);
		}

		@Override
		public final Object visitIf(Nez.IfCondition e, Object a) {
			return Symbolstate.Unchanged;
		}

		@Override
		public final Object visitOn(Nez.OnCondition e, Object a) {
			return Symbolstate.Mutated;
		}

		// @Override
		// public final Object visitSetCount(Nez.SetCount e, Object a) {
		// return this.inferSymbolstate(e.get(0));
		// }
		//
		// @Override
		// public final Object visitCount(Nez.Count e, Object a) {
		// return this.inferSymbolstate(e.get(0));
		// }

	}
}
