package nez.lang;

import nez.lang.Nez.And;
import nez.lang.Nez.Any;
import nez.lang.Nez.BeginTree;
import nez.lang.Nez.BlockScope;
import nez.lang.Nez.Byte;
import nez.lang.Nez.ByteSet;
import nez.lang.Nez.Choice;
import nez.lang.Nez.Detree;
import nez.lang.Nez.Empty;
import nez.lang.Nez.EndTree;
import nez.lang.Nez.Fail;
import nez.lang.Nez.FoldTree;
import nez.lang.Nez.IfCondition;
import nez.lang.Nez.Label;
import nez.lang.Nez.LinkTree;
import nez.lang.Nez.LocalScope;
import nez.lang.Nez.Not;
import nez.lang.Nez.OnCondition;
import nez.lang.Nez.OneMore;
import nez.lang.Nez.Option;
import nez.lang.Nez.Pair;
import nez.lang.Nez.Replace;
import nez.lang.Nez.Sequence;
import nez.lang.Nez.SymbolAction;
import nez.lang.Nez.SymbolExists;
import nez.lang.Nez.SymbolMatch;
import nez.lang.Nez.SymbolPredicate;
import nez.lang.Nez.Tag;
import nez.lang.Nez.ZeroMore;
import nez.util.Verbose;

public enum ByteAcceptance {
	Accept, Unconsumed, Reject;

	static Analyzer analyzer = new Analyzer();

	public final static ByteAcceptance acc(Expression e, int ch) {
		return analyzer.accept(e, ch);
	}

	public final static boolean isDisjoint(Expression e, Expression e2) {
		for (int ch = 0; ch < 256; ch++) {
			if (acc(e, ch) == Reject) {
				continue;
			}
			if (acc(e2, ch) == Reject) {
				continue;
			}
			return false;
		}
		return true;
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
			return (e.byteset[(int) ch]) ? Accept : Reject;
		}

		@Override
		public ByteAcceptance visitAny(Any e, Object ch) {
			return ((int) ch == 0) ? Reject : Accept;
			// return Accept;
		}

		@Override
		public ByteAcceptance visitMultiByte(Nez.MultiByte e, Object ch) {
			return ((e.byteseq[0] & 0xff) == (int) ch) ? Accept : Reject;
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
		public ByteAcceptance visitDispatch(Nez.Dispatch e, Object ch) {
			return accept(e.get(e.indexMap[(int) ch]), ch);
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
		public ByteAcceptance visitBeginTree(BeginTree e, Object ch) {
			return Unconsumed;
		}

		@Override
		public ByteAcceptance visitFoldTree(FoldTree e, Object ch) {
			return Unconsumed;
		}

		@Override
		public ByteAcceptance visitLinkTree(LinkTree e, Object ch) {
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
		public ByteAcceptance visitEndTree(EndTree e, Object ch) {
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
		public ByteAcceptance visitScan(Nez.Scan e, Object ch) {
			return accept(e.get(0), ch);
		}

		@Override
		public ByteAcceptance visitRepeat(Nez.Repeat e, Object ch) {
			return accept(e.get(0), ch);
		}

		@Override
		public ByteAcceptance visitIf(IfCondition e, Object ch) {
			return Unconsumed;
		}

		@Override
		public ByteAcceptance visitOn(OnCondition e, Object ch) {
			return accept(e.get(0), ch);
		}

		@Override
		public Object visitLabel(Label e, Object a) {
			return Unconsumed;
		}
	}
}
