package nez.lang.regex;

import java.io.IOException;

import nez.Grammar;
import nez.Parser;
import nez.ParserStrategy;
import nez.ast.Tree;
import nez.lang.Expression;
import nez.lang.GrammarFileLoader;
import nez.lang.Production;
import nez.lang.expr.ExpressionCommons;
import nez.util.ConsoleUtils;
import nez.util.StringUtils;
import nez.util.UList;

public class RegularExpressionLoader extends GrammarFileLoader {

	public RegularExpressionLoader() {
		init(RegularExpressionLoader.class, new Undefined());
	}

	public class Undefined extends DefaultVisitor {
		@Override
		public Expression toExpression(Tree<?> node) {
			ConsoleUtils.println(node.formatSourceMessage("error", "unsupproted in RegularExpressionLoader #" + node));
			return null;
		}

		@Override
		public Expression toExpression(Tree<?> node, Expression expr) {
			ConsoleUtils.println(node.formatSourceMessage("error", "unsupproted in RegularExpressionLoader #" + node));
			return null;
		}

		@Override
		public Expression toExpression(Tree<?> node, Expression expr1, Expression expr2) {
			ConsoleUtils.println(node.formatSourceMessage("error", "unsupproted in RegularExpressionLoader #" + node));
			return null;
		}
	}

	static Parser lParser;

	@Override
	public Parser getLoaderParser(String start) {
		if (lParser == null) {
			try {
				ParserStrategy option = ParserStrategy.newSafeStrategy();
				Grammar g = GrammarFileLoader.loadGrammar("regex.nez", option);
				// g.dump();
				lParser = g.newParser(option);
				strategy.report();
			} catch (IOException e) {
				ConsoleUtils.exit(1, "unload: " + e.getMessage());
			}
			assert (lParser != null);
		}
		return lParser;
	}

	@Override
	public void parse(Tree<?> node) {
		Production p = this.getGrammar().newProduction("Start", null);
		p.setExpression(pi(node, null));
	}

	private final Expression pi(Tree<?> expr, Expression k) {
		return find(expr.getTag().toString()).toExpression(expr, k);
	}

	private final Expression getExpression(Tree<?> node) {
		return find(node.getTag().toString()).toExpression(node);
	}

	// boolean isSOS = false;

	public class _Pattern extends Undefined {
		@Override
		public Expression toExpression(Tree<?> e, Expression k) {
			String ruleName = "Pattern";
			Expression ne = ExpressionCommons.newNonTerminal(e, getGrammar(), ruleName);
			getGrammar().newProduction(ruleName, pi(e.get(0), k));
			Expression zeroMore = ExpressionCommons.newPzero(null, toSeq(null, ExpressionCommons.newPnot(null, ne), toAny(null)));
			Expression main = toSeq(null, zeroMore, ExpressionCommons.newPone(null, toSeq(null, ne, zeroMore)));
			// ruleName = "SOSPattern";
			// isSOS = true;
			// Expression sose = ExpressionCommons.newNonTerminal(e,
			// this.getGrammar(), ruleName);
			// getGrammar().newProduction(ruleName, pi(e.get(0), k));
			// Expression sos = toSeq(null, sose,
			// ExpressionCommons.newPzero(null,
			// toSeq(null, zeroMore, ne)));
			// return toChoice(null, sos, main);
			return main;
		}
	}

	// pi(e, k) e: regular expression, k: continuation
	// pi(e1|e2, k) = pi(e1, k) / pi(e2, k)
	public class _Or extends Undefined {
		@Override
		public Expression toExpression(Tree<?> e, Expression k) {
			return toChoice(e, pi(e.get(0), k), pi(e.get(1), k));
		}
	}

	// pi(e1e2, k) = pi(e1, pi(e2, k))
	public class Concatenation extends Undefined {
		@Override
		public Expression toExpression(Tree<?> e, Expression k) {
			return pi(e.get(0), pi(e.get(1), k));
		}
	}

	// pi((?>e), k) = pi(e, "") k
	public class IndependentExpr extends Undefined {
		@Override
		public Expression toExpression(Tree<?> e, Expression k) {
			return toSeq(e, pi(e.get(0), toEmpty(e)), k);
		}
	}

	// pi((?=e), k) = &pi(e, "") k
	public class _And extends Undefined {
		@Override
		public Expression toExpression(Tree<?> e, Expression k) {
			return toSeq(e, ExpressionCommons.newPand(null, pi(e.get(0), toEmpty(e))), k);
		}
	}

	// pi((?!e), k) = !pi(e, "") k
	public class _Not extends Undefined {
		@Override
		public Expression toExpression(Tree<?> e, Expression k) {
			return toSeq(e, ExpressionCommons.newPnot(null, pi(e.get(0), toEmpty(e))), k);
		}
	}

	// pi(e*+, k) = pi(e*, "") k
	public class PossessiveRepetition extends Undefined {
		@Override
		public Expression toExpression(Tree<?> e, Expression k) {
			return toSeq(e, piRepetition(e, toEmpty(e)), k);
		}
	}

	int NonTerminalCount = 0;

	// pi(e*?, k) = A, A <- k / pi(e, A)
	public class LazyQuantifiers extends Undefined {
		@Override
		public Expression toExpression(Tree<?> e, Expression k) {
			String ruleName = "Repetition" + NonTerminalCount++;
			Expression ne = ExpressionCommons.newNonTerminal(e, getGrammar(), ruleName);
			if (k == null) {
				k = ExpressionCommons.newEmpty(null);
			}
			getGrammar().newProduction(ruleName, toChoice(e, k, pi(e.get(0), ne)));
			return ne;
		}
	}

	// pi(e*, k) = A, A <- pi(e, A) / k
	public class _Repetition extends Undefined {
		@Override
		public Expression toExpression(Tree<?> e, Expression k) {
			String ruleName = "Repetition" + NonTerminalCount++;
			Expression ne = ExpressionCommons.newNonTerminal(e, getGrammar(), ruleName);
			getGrammar().newProduction(ruleName, toChoice(e, pi(e.get(0), ne), k));
			return ne;
		}
	}

	// pi(e?, k) = pi(e, k) / k
	public class _Option extends Undefined {
		@Override
		public Expression toExpression(Tree<?> e, Expression k) {
			return toChoice(e, pi(e.get(0), k), k);
		}
	}

	public class _OneMoreRepetition extends Undefined {
		@Override
		public Expression toExpression(Tree<?> e, Expression k) {
			return pi(e.get(0), piRepetition(e, k));
		}
	}

	// pi(e{3}, k) = pi(e, pi(e, pi(e, k)))
	public class NTimesRepetition extends Undefined {
		@Override
		public Expression toExpression(Tree<?> e, Expression k) {
			final int N = e.getInt(1, 0);
			Expression ne = k;
			for (int i = 0; i < N; i++) {
				ne = pi(e.get(0), ne);
			}
			return ne;
		}
	}

	// pi(e{N,}, k) = pi(e{N}, A), A <- pi(e, A) / k
	public class NMoreRepetition extends Undefined {
		@Override
		public Expression toExpression(Tree<?> e, Expression k) {
			return find("NTimesRepetition").toExpression(e, piRepetition(e, k));
		}
	}

	// pi(e{N,M}, k) = pi(e{N}, A0), A0 <- pi(e, A1) / k, ... A(M-N) <- k
	public class NtoMTimesRepetition extends Undefined {
		@Override
		public Expression toExpression(Tree<?> e, Expression k) {
			final int DIF = e.getInt(2, 0) - e.getInt(1, 0);
			String ruleName = "Repetition" + NonTerminalCount++;
			Expression ne = ExpressionCommons.newNonTerminal(e, getGrammar(), ruleName);
			getGrammar().newProduction(ruleName, k);
			for (int i = 0; i < DIF; i++) {
				ruleName = "Repetition" + NonTerminalCount++;
				Expression nne = ne;
				ne = ExpressionCommons.newNonTerminal(e, getGrammar(), ruleName);
				getGrammar().newProduction(ruleName, toChoice(e, pi(e.get(0), nne), k));
			}
			return find("NTimesRepetition").toExpression(e, ne);
		}
	}

	public class _Any extends Undefined {

		@Override
		public Expression toExpression(Tree<?> e) {
			return ExpressionCommons.newCany(null, false);
		}

		@Override
		public Expression toExpression(Tree<?> e, Expression k) {
			return toSeq(e, k);
		}
	}

	public class _Capture extends Undefined {
		@Override
		public Expression toExpression(Tree<?> e, Expression k) {
			return pi(e.get(0), k); // TODO Capture
		}
	}

	public class NegativeCharacterSet extends Undefined {
		@Override
		public Expression toExpression(Tree<?> e, Expression k) {
			Expression nce = toSeq(e, ExpressionCommons.newPnot(e, toCharacterSet(e)), toAny(e));
			return toSeq(e, nce, k);
		}
	}

	public class CharacterSet extends Undefined {
		@Override
		public Expression toExpression(Tree<?> e) {
			UList<Expression> l = new UList<Expression>(new Expression[e.size()]);
			byteMap = new boolean[257];
			for (Tree<?> subnode : e) {
				ExpressionCommons.addChoice(l, getExpression(subnode));
			}
			return ExpressionCommons.newCset(null, false, byteMap);
		}

		@Override
		public Expression toExpression(Tree<?> e, Expression k) {
			return toSeq(e, k);
		}
	}

	public class CharacterRange extends Undefined {

		@Override
		public Expression toExpression(Tree<?> e) {
			byte[] begin = StringUtils.toUtf8(e.get(0).toText());
			byte[] end = StringUtils.toUtf8(e.get(1).toText());
			if (byteMap == null) {
				byteMap = new boolean[257];
			}
			for (byte i = begin[0]; i <= end[0]; i++) {
				byteMap[i] = true;
			}
			return ExpressionCommons.newCharSet(null, e.get(0).toText(), e.get(1).toText());
		}

		@Override
		public Expression toExpression(Tree<?> e, Expression k) {
			return toSeq(e, k);
		}
	}

	public class CharacterSetItem extends Undefined {
		@Override
		public Expression toExpression(Tree<?> e) {
			byte[] utf8 = StringUtils.toUtf8(e.toText());
			byteMap[utf8[0]] = true;
			return ExpressionCommons.newCbyte(null, false, utf8[0]);
		}

		@Override
		public Expression toExpression(Tree<?> e, Expression k) {
			return toSeq(e, k);
		}
	}

	// pi(c, k) = c k
	// c: single character
	public class Character extends Undefined {
		@Override
		public Expression toExpression(Tree<?> e) {
			String text = e.toText();
			byte[] utf8 = StringUtils.toUtf8(text);
			if (utf8.length != 1) {
				ConsoleUtils.exit(1, "Error: not Character Literal");
			}
			return ExpressionCommons.newCbyte(null, false, utf8[0]);
		}

		@Override
		public Expression toExpression(Tree<?> e, Expression k) {
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

	public class EndOfString extends Undefined {
		@Override
		public Expression toExpression(Tree<?> e, Expression k) {
			return pi(e.get(0), toSeq(null, ExpressionCommons.newPnot(null, toAny(null)), k));
		}
	}

	boolean byteMap[];

	// boolean useByteMap = true;

	public class Empty extends Undefined {
		@Override
		public Expression toExpression(Tree<?> e, Expression k) {
			return ExpressionCommons.newEmpty(null);
		}
	}

	public class _Choice extends Undefined {
		@Override
		public Expression toExpression(Tree<?> node, Expression e, Expression k) {
			UList<Expression> l = new UList<Expression>(new Expression[2]);
			ExpressionCommons.addChoice(l, e);
			if (k != null) {
				ExpressionCommons.addChoice(l, k);
			} else {
				ExpressionCommons.addChoice(l, toEmpty(node));
			}
			return ExpressionCommons.newPchoice(null, l);
		}
	}

	public class _Seq extends Undefined {
		@Override
		public Expression toExpression(Tree<?> e, Expression k) {
			UList<Expression> l = new UList<Expression>(new Expression[2]);
			ExpressionCommons.addSequence(l, getExpression(e));
			if (k != null) {
				ExpressionCommons.addSequence(l, k);
			}
			return ExpressionCommons.newPsequence(null, l);
		}

		@Override
		public Expression toExpression(Tree<?> node, Expression e, Expression k) {
			UList<Expression> l = new UList<Expression>(new Expression[2]);
			ExpressionCommons.addSequence(l, e);
			if (k != null) {
				ExpressionCommons.addSequence(l, k);
			}
			return ExpressionCommons.newPsequence(null, l);
		}
	}

	private final Expression toEmpty(Tree<?> node) {
		return find("Empty").toExpression(node);
	}

	private final Expression toAny(Tree<?> node) {
		return find("Any").toExpression(node);
	}

	private final Expression toCharacterSet(Tree<?> node) {
		return find("CharacterSet").toExpression(node);
	}

	private final Expression piRepetition(Tree<?> node, Expression k) {
		return find("Repetition").toExpression(node, k);
	}

	private final Expression toSeq(Tree<?> node, Expression k) {
		return find("Seq").toExpression(node, k);
	}

	private final Expression toSeq(Tree<?> node, Expression e, Expression k) {
		return find("Seq").toExpression(node, e, k);
	}

	private final Expression toChoice(Tree<?> node, Expression e, Expression k) {
		return find("Choice").toExpression(node, e, k);
	}

	private final Expression _LineTerminator() {
		Expression l[] = { ExpressionCommons.newCbyte(null, false, '\n'), ExpressionCommons.newCbyte(null, false, '\r'), ExpressionCommons.newString(null, "\r\n"), };
		return ExpressionCommons.newPchoice(null, new UList<>(l));
	}
}
