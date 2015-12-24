package nez.lang;

import nez.lang.Nez.And;
import nez.lang.Nez.Any;
import nez.lang.Nez.BlockScope;
import nez.lang.Nez.Byte;
import nez.lang.Nez.ByteSet;
import nez.lang.Nez.Choice;
import nez.lang.Nez.Detree;
import nez.lang.Nez.Empty;
import nez.lang.Nez.Fail;
import nez.lang.Nez.If;
import nez.lang.Nez.LeftFold;
import nez.lang.Nez.Link;
import nez.lang.Nez.LocalScope;
import nez.lang.Nez.New;
import nez.lang.Nez.Not;
import nez.lang.Nez.On;
import nez.lang.Nez.OneMore;
import nez.lang.Nez.Option;
import nez.lang.Nez.Pair;
import nez.lang.Nez.PreNew;
import nez.lang.Nez.Replace;
import nez.lang.Nez.Sequence;
import nez.lang.Nez.String;
import nez.lang.Nez.SymbolAction;
import nez.lang.Nez.SymbolExists;
import nez.lang.Nez.SymbolMatch;
import nez.lang.Nez.SymbolPredicate;
import nez.lang.Nez.Tag;
import nez.lang.Nez.ZeroMore;
import nez.lang.expr.NonTerminal;
import nez.util.Verbose;

public enum ByteAcceptance {
	Accept, Unconsumed, Reject;

	static Analyzer analyzer = new Analyzer();

	public final static ByteAcceptance acc(Expression e, int ch) {
		return analyzer.accept(e, ch);
	}

	public final static class Analyzer extends Expression.Visitor {

		public ByteAcceptance accept(Expression e, Object ch) {
			return (ByteAcceptance) e.visit(this, ch);
		}

		@Override
		public ByteAcceptance visitNonTerminal(NonTerminal e, Object ch) {
			try {
				return accept(e.deReference(), ch);
			} catch (StackOverflowError ex) {
				Verbose.debug(e + " at " + e.getLocalName());
			}
			return Accept;
		}

		@Override
		public ByteAcceptance visitEmpty(Empty e, Object ch) {
			return Unconsumed;
		}

		@Override
		public ByteAcceptance visitFail(Fail e, Object ch) {
			return Reject;
		}

		@Override
		public ByteAcceptance visitByte(Byte e, Object ch) {
			return (e.byteChar == (int) ch) ? Accept : Reject;
		}

		@Override
		public ByteAcceptance visitByteSet(ByteSet e, Object ch) {
			return (e.byteMap[(int) ch]) ? Accept : Reject;
		}

		@Override
		public ByteAcceptance visitAny(Any e, Object ch) {
			return ((int) ch == 0) ? Reject : Accept;
			// return Accept;
		}

		@Override
		public ByteAcceptance visitString(String e, Object ch) {
			return ((e.byteSeq[0] & 0xff) == (int) ch) ? Accept : Reject;
		}

		@Override
		public ByteAcceptance visitPair(Pair e, Object ch) {
			ByteAcceptance r = accept(e.get(0), ch);
			if (r == Unconsumed) {
				return accept(e.get(1), ch);
			}
			return r;
		}

		@Override
		public ByteAcceptance visitSequence(Sequence e, Object ch) {
			for (int i = 0; i < e.size(); i++) {
				ByteAcceptance r = accept(e.get(i), ch);
				if (r != Unconsumed) {
					return r;
				}
			}
			return Unconsumed;
		}

		@Override
		public ByteAcceptance visitChoice(Choice e, Object ch) {
			boolean hasUnconsumed = false;
			for (int i = 0; i < e.size(); i++) {
				ByteAcceptance r = accept(e.get(i), ch);
				if (r == Accept) {
					return r;
				}
				if (r == Unconsumed) {
					hasUnconsumed = true;
				}
			}
			return hasUnconsumed ? Unconsumed : Reject;
		}

		@Override
		public ByteAcceptance visitOption(Option e, Object ch) {
			ByteAcceptance r = accept(e.get(0), ch);
			return (r == Accept) ? r : Unconsumed;
		}

		@Override
		public ByteAcceptance visitZeroMore(ZeroMore e, Object ch) {
			ByteAcceptance r = accept(e.get(0), ch);
			return (r == Accept) ? r : Unconsumed;
		}

		@Override
		public ByteAcceptance visitOneMore(OneMore e, Object ch) {
			return accept(e.get(0), ch);
		}

		@Override
		public ByteAcceptance visitAnd(And e, Object ch) {
			ByteAcceptance r = accept(e.get(0), ch);
			return (r == Reject) ? r : Unconsumed;
		}

		@Override
		public ByteAcceptance visitNot(Not e, Object ch) {
			Expression inner = e.get(0);
			if (inner instanceof Nez.Byte || inner instanceof Nez.ByteSet || inner instanceof Nez.Any) {
				return accept(inner, ch) == Accept ? Reject : Unconsumed;
			}
			/* The code below works only if a single character in !(e) */
			/* we must accept 'i' for !'int' 'i' */
			return Unconsumed;
		}

		@Override
		public ByteAcceptance visitPreNew(PreNew e, Object ch) {
			return Unconsumed;
		}

		@Override
		public ByteAcceptance visitLeftFold(LeftFold e, Object ch) {
			return Unconsumed;
		}

		@Override
		public ByteAcceptance visitLink(Link e, Object ch) {
			return accept(e.get(0), ch);
		}

		@Override
		public ByteAcceptance visitTag(Tag e, Object ch) {
			return Unconsumed;
		}

		@Override
		public ByteAcceptance visitReplace(Replace e, Object ch) {
			return Unconsumed;
		}

		@Override
		public ByteAcceptance visitNew(New e, Object ch) {
			return Unconsumed;
		}

		@Override
		public ByteAcceptance visitDetree(Detree e, Object ch) {
			return accept(e.get(0), ch);
		}

		@Override
		public ByteAcceptance visitBlockScope(BlockScope e, Object ch) {
			return accept(e.get(0), ch);
		}

		@Override
		public ByteAcceptance visitLocalScope(LocalScope e, Object ch) {
			return accept(e.get(0), ch);
		}

		@Override
		public ByteAcceptance visitSymbolAction(SymbolAction e, Object ch) {
			return accept(e.get(0), ch);
		}

		@Override
		public ByteAcceptance visitSymbolPredicate(SymbolPredicate e, Object ch) {
			return accept(e.get(0), ch);
		}

		@Override
		public ByteAcceptance visitSymbolMatch(SymbolMatch e, Object ch) {
			return accept(e.get(0), ch); // Accept
		}

		@Override
		public ByteAcceptance visitSymbolExists(SymbolExists e, Object ch) {
			return Unconsumed;
		}

		@Override
		public ByteAcceptance visitIf(If e, Object ch) {
			return Unconsumed;
		}

		@Override
		public ByteAcceptance visitOn(On e, Object ch) {
			return accept(e.get(0), ch);
		}
	}
}
