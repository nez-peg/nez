package nez.lang;

import nez.lang.Nez.Byte;
import nez.lang.expr.NonTerminal;

public enum Consumer {
	Unconsumed, //
	Consumed, //
	Undecided;

	public final static class Analyzer extends Expression.Visitor {

		public Consumer quickCheck(Production p) {
			String uname = p.getUniqueName();
			if (this.isVisited(uname)) {
				return (Consumer) lookup(uname);
			}
			Consumer c = quickCheck(p.getExpression());
			if (c != Undecided) {
				this.memo(uname, c);
			}
			return c;
		}

		public Consumer deepCheck(Production p) {
			String uname = p.getUniqueName();
			if (this.isVisited(uname)) {
				return (Consumer) lookup(uname);
			}
			Consumer c = quickCheck(p.getExpression());
			this.memo(uname, c);
			if (c == Undecided) {
				c = deepCheck(p.getExpression());
				this.memo(uname, c);
			}
			return c;
		}

		public final Consumer quickCheck(Expression e) {
			return (Consumer) e.visit(this, true);
		}

		public final Consumer deepCheck(Expression e) {
			return (Consumer) e.visit(this, false);
		}

		public final boolean isConsumed(Production p) {
			Consumer c = deepCheck(p);
			return c == Consumed;
		}

		public final boolean isConsumed(Expression p) {
			Consumer c = deepCheck(p);
			return c == Consumed;
		}

		private Consumer check(Expression e, Object a) {
			return (Consumer) e.visit(this, a);
		}

		@Override
		public Object visitNonTerminal(NonTerminal e, Object a) {
			if ((Boolean) a) {
				return Undecided;
			}
			return this.deepCheck(e.getProduction());
		}

		@Override
		public Object visitEmpty(Nez.Empty e, Object a) {
			return Unconsumed;
		}

		@Override
		public Object visitFail(Nez.Fail e, Object a) {
			return Unconsumed;
		}

		@Override
		public Object visitByte(Byte e, Object a) {
			return Consumed;
		}

		@Override
		public Object visitByteSet(Nez.ByteSet e, Object a) {
			return Consumed;
		}

		@Override
		public Object visitAny(Nez.Any e, Object a) {
			return Consumed;
		}

		@Override
		public Object visitString(Nez.String e, Object a) {
			return Consumed;
		}

		@Override
		public Object visitPair(Nez.Pair e, Object a) {
			if (check(e.getFirst(), a) == Consumed) {
				return Consumed;
			}
			return check(e.getNext(), a);
		}

		@Override
		public Object visitSequence(Nez.Sequence e, Object a) {
			boolean undecided = false;
			for (Expression sub : e) {
				Consumer c = check(sub, a);
				if (c == Consumed) {
					return Consumed;
				}
				if (c == Undecided) {
					undecided = true;
				}
			}
			return undecided ? Undecided : Unconsumed;
		}

		@Override
		public Object visitChoice(Nez.Choice e, Object a) {
			boolean unconsumed = false;
			boolean undecided = false;
			for (Expression sub : e) {
				Consumer c = check(sub, a);
				if (c == Consumed) {
					continue;
				}
				unconsumed = true;
				if (c == Undecided) {
					undecided = true;
				}
			}
			if (!unconsumed) {
				return Consumed;
			}
			return undecided ? Undecided : Unconsumed;
		}

		@Override
		public Object visitOption(Nez.Option e, Object a) {
			return Unconsumed;
		}

		@Override
		public Object visitZeroMore(Nez.ZeroMore e, Object a) {
			return Unconsumed;
		}

		@Override
		public Object visitOneMore(Nez.OneMore e, Object a) {
			return check(e.get(0), a);
		}

		@Override
		public Object visitAnd(Nez.And e, Object a) {
			return Unconsumed;
		}

		@Override
		public Object visitNot(Nez.Not e, Object a) {
			return Unconsumed;
		}

		@Override
		public Object visitPreNew(Nez.PreNew e, Object a) {
			return Unconsumed;
		}

		@Override
		public Object visitLeftFold(Nez.LeftFold e, Object a) {
			return Unconsumed;
		}

		@Override
		public Object visitLink(Nez.Link e, Object a) {
			return check(e.get(0), a);
		}

		@Override
		public Object visitTag(Nez.Tag e, Object a) {
			return Unconsumed;
		}

		@Override
		public Object visitReplace(Nez.Replace e, Object a) {
			return Unconsumed;
		}

		@Override
		public Object visitNew(Nez.New e, Object a) {
			return Unconsumed;
		}

		@Override
		public Object visitDetree(Nez.Detree e, Object a) {
			return check(e.get(0), a);
		}

		@Override
		public Object visitBlockScope(Nez.BlockScope e, Object a) {
			return check(e.get(0), a);
		}

		@Override
		public Object visitLocalScope(Nez.LocalScope e, Object a) {
			return check(e.get(0), a);
		}

		@Override
		public Object visitSymbolAction(Nez.SymbolAction e, Object a) {
			return check(e.get(0), a);
		}

		@Override
		public Object visitSymbolMatch(Nez.SymbolMatch e, Object a) {
			return Undecided;
		}

		@Override
		public Object visitSymbolPredicate(Nez.SymbolPredicate e, Object a) {
			return check(e.get(0), a);
		}

		@Override
		public Object visitSymbolExists(Nez.SymbolExists e, Object a) {
			return check(e.get(0), a);
		}

		@Override
		public Object visitIf(Nez.If e, Object a) {
			return Unconsumed;
		}

		@Override
		public Object visitOn(Nez.On e, Object a) {
			return Unconsumed;
		}
	}
}
