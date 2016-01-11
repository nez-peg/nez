package nez.lang.regex;

import java.util.List;

import nez.ast.Tree;
import nez.lang.Expression;
import nez.lang.Expressions;
import nez.lang.Grammar;
import nez.lang.ast.ExpressionConstructor;
import nez.lang.ast.ExpressionTransducer;
import nez.lang.ast.GrammarVisitorMap;
import nez.lang.ast.NezSymbols;
import nez.parser.ParserStrategy;
import nez.util.ConsoleUtils;
import nez.util.StringUtils;
import nez.util.UList;

public class RegularExpressionConstructor extends GrammarVisitorMap<ExpressionTransducer> implements ExpressionConstructor, NezSymbols {

	public RegularExpressionConstructor(Grammar grammar, ParserStrategy strategy) {
		super(grammar, strategy);
		init(RegularExpressionConstructor.class, new TreeVisitor());
	}

	@Override
	public Expression newInstance(Tree<?> node) {
		return this.find(key(node)).accept(node, null);
	}

	public Expression pi(Tree<?> node, Expression next) {
		return this.find(key(node)).accept(node, next);
	}

	public class TreeVisitor implements ExpressionTransducer {
		@Override
		public Expression accept(Tree<?> node, Expression e) {
			undefined(node);
			return null;
		}
	}

	// boolean isSOS = false;

	public class _Pattern extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> e, Expression k) {
			String ruleName = "Pattern";
			Expression ne = Expressions.newNonTerminal(e, getGrammar(), ruleName);
			getGrammar().addProduction(ruleName, pi(e.get(0), k));
			Expression zeroMore = Expressions.newZeroMore(null, newPair(Expressions.newNot(null, ne), toAny(null)));
			Expression main = newPair(zeroMore, Expressions.newOneMore(null, newPair(ne, zeroMore)));
			return main;
		}
	}

	// pi(e, k) e: regular expression, k: continuation
	// pi(e1|e2, k) = pi(e1, k) / pi(e2, k)
	public class _Or extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> e, Expression k) {
			return newChoice(pi(e.get(0), k), pi(e.get(1), k));
		}
	}

	// pi(e1e2, k) = pi(e1, pi(e2, k))
	public class Concatenation extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> e, Expression k) {
			return pi(e.get(0), pi(e.get(1), k));
		}
	}

	// pi((?>e), k) = pi(e, "") k
	public class IndependentExpr extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> e, Expression k) {
			return newPair(pi(e.get(0), Expressions.newEmpty()), k);
		}
	}

	// pi((?=e), k) = &pi(e, "") k
	public class _And extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> e, Expression k) {
			return newPair(Expressions.newAnd(null, pi(e.get(0), Expressions.newEmpty())), k);
		}
	}

	// pi((?!e), k) = !pi(e, "") k
	public class _Not extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> e, Expression k) {
			return newPair(Expressions.newNot(null, pi(e.get(0), Expressions.newEmpty())), k);
		}
	}

	// pi(e*+, k) = pi(e*, "") k
	public class PossessiveRepetition extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> e, Expression k) {
			return newPair(piRepetition(e, Expressions.newEmpty()), k);
		}
	}

	int NonTerminalCount = 0;

	// pi(e*?, k) = A, A <- k / pi(e, A)
	public class LazyQuantifiers extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> e, Expression k) {
			String ruleName = "Repetition" + NonTerminalCount++;
			Expression ne = Expressions.newNonTerminal(e, getGrammar(), ruleName);
			if (k == null) {
				k = Expressions.newEmpty(null);
			}
			getGrammar().addProduction(ruleName, newChoice(k, pi(e.get(0), ne)));
			return ne;
		}
	}

	// pi(e*, k) = A, A <- pi(e, A) / k
	public class _Repetition extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> e, Expression k) {
			String ruleName = "Repetition" + NonTerminalCount++;
			Expression ne = Expressions.newNonTerminal(e, getGrammar(), ruleName);
			getGrammar().addProduction(ruleName, newChoice(pi(e.get(0), ne), k));
			return ne;
		}
	}

	// pi(e?, k) = pi(e, k) / k
	public class _Option extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> e, Expression k) {
			return newChoice(pi(e.get(0), k), k);
		}
	}

	public class _OneMoreRepetition extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> e, Expression k) {
			return pi(e.get(0), piRepetition(e, k));
		}
	}

	// pi(e{3}, k) = pi(e, pi(e, pi(e, k)))
	public class NTimesRepetition extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> e, Expression k) {
			final int N = e.getInt(1, 0);
			Expression ne = k;
			for (int i = 0; i < N; i++) {
				ne = pi(e.get(0), ne);
			}
			return ne;
		}
	}

	// pi(e{N,}, k) = pi(e{N}, A), A <- pi(e, A) / k
	public class NMoreRepetition extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> e, Expression k) {
			return find("NTimesRepetition").accept(e, piRepetition(e, k));
		}
	}

	// pi(e{N,M}, k) = pi(e{N}, A0), A0 <- pi(e, A1) / k, ... A(M-N) <- k
	public class NtoMTimesRepetition extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> e, Expression k) {
			final int DIF = e.getInt(2, 0) - e.getInt(1, 0);
			String ruleName = "Repetition" + NonTerminalCount++;
			Expression ne = Expressions.newNonTerminal(e, getGrammar(), ruleName);
			getGrammar().addProduction(ruleName, k);
			for (int i = 0; i < DIF; i++) {
				ruleName = "Repetition" + NonTerminalCount++;
				Expression nne = ne;
				ne = Expressions.newNonTerminal(e, getGrammar(), ruleName);
				getGrammar().addProduction(ruleName, newChoice(pi(e.get(0), nne), k));
			}
			return find("NTimesRepetition").accept(e, ne);
		}
	}

	public class _Any extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> e, Expression k) {
			if (k == null) {
				return Expressions.newAny(e);
			}
			return toSeq(e, k);
		}
	}

	public class _Capture extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> e, Expression k) {
			return pi(e.get(0), k); // TODO Capture
		}
	}

	public class NegativeCharacterSet extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> e, Expression k) {
			Expression nce = newPair(Expressions.newNot(e, toCharacterSet(e)), toAny(e));
			return newPair(nce, k);
		}
	}

	public class CharacterSet extends TreeVisitor {

		@Override
		public Expression accept(Tree<?> e, Expression k) {
			if (k == null) {
				UList<Expression> l = new UList<>(new Expression[e.size()]);
				byteMap = new boolean[257];
				for (Tree<?> subnode : e) {
					Expressions.addChoice(l, newInstance(subnode));
				}
				return Expressions.newByteSet(null, byteMap);
			}
			return toSeq(e, k);
		}
	}

	public class CharacterRange extends TreeVisitor {

		@Override
		public Expression accept(Tree<?> e, Expression k) {
			if (k == null) {
				byte[] begin = StringUtils.toUtf8(e.get(0).toText());
				byte[] end = StringUtils.toUtf8(e.get(1).toText());
				if (byteMap == null) {
					byteMap = new boolean[257];
				}
				for (byte i = begin[0]; i <= end[0]; i++) {
					byteMap[i] = true;
				}
				return Expressions.newCharSet(null, e.get(0).toText(), e.get(1).toText());
			}
			return toSeq(e, k);
		}
	}

	public class CharacterSetItem extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> e, Expression k) {
			if (k == null) {
				byte[] utf8 = StringUtils.toUtf8(e.toText());
				byteMap[utf8[0]] = true;
				return Expressions.newByte(null, utf8[0]);
			}
			return toSeq(e, k);
		}
	}

	// pi(c, k) = c k
	// c: single character
	public class Character extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> e, Expression k) {
			if (k == null) {
				String text = e.toText();
				byte[] utf8 = StringUtils.toUtf8(text);
				if (utf8.length != 1) {
					ConsoleUtils.exit(1, "Error: not Character Literal");
				}
				return Expressions.newByte(null, utf8[0]);
			}
			return toSeq(e, k);
		}
	}

	// TODO modify implementation
	// public Expression piStartOfString(Tree<?> e, Expression k) {
	// if (isSOS) {
	// return pi(e.get(0), k);
	// }
	// return ExpressionCommons.newFailure(null);
	// }

	public class EndOfString extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> e, Expression k) {
			return pi(e.get(0), newPair(Expressions.newNot(null, toAny(null)), k));
		}
	}

	boolean byteMap[];

	// boolean useByteMap = true;

	public class Empty extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> e, Expression k) {
			return Expressions.newEmpty(null);
		}
	}

	// public class _Choice extends TreeVisitor {
	// @Override
	// public Expression accept(Tree<?> node, Expression e, Expression k) {
	// UList<Expression> l = new UList<Expression>(new Expression[2]);
	// Expressions.addChoice(l, e);
	// if (k != null) {
	// Expressions.addChoice(l, k);
	// } else {
	// Expressions.addChoice(l, toEmpty(node));
	// }
	// return Expressions.newChoice(l);
	// }
	// }

	public class _Seq extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> e, Expression k) {
			UList<Expression> l = new UList<>(new Expression[2]);
			Expressions.addSequence(l, newInstance(e));
			if (k != null) {
				Expressions.addSequence(l, k);
			}
			return Expressions.newPair(l);
		}
	}

	private final Expression toEmpty(Tree<?> node) {
		return find("Empty").accept(node, null);
	}

	private final Expression toAny(Tree<?> node) {
		return find("Any").accept(node, null);
	}

	private final Expression toCharacterSet(Tree<?> node) {
		return find("CharacterSet").accept(node, null);
	}

	private final Expression piRepetition(Tree<?> node, Expression k) {
		return find("Repetition").accept(node, k);
	}

	private final Expression toSeq(Tree<?> node, Expression k) {
		return find("Seq").accept(node, k);
	}

	private final Expression newPair(Expression e, Expression k) {
		List<Expression> l = Expressions.newList(2);
		Expressions.addSequence(l, e);
		if (k != null) {
			Expressions.addSequence(l, k);
		}
		return Expressions.newPair(l);
	}

	private final Expression newChoice(Expression e, Expression k) {
		List<Expression> l = Expressions.newList(2);
		Expressions.addChoice(l, e);
		if (k != null) {
			Expressions.addChoice(l, k);
		} else {
			Expressions.addChoice(l, Expressions.newEmpty());
		}
		return Expressions.newChoice(l);
	}
}
