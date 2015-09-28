package nez.lang.regex;

import java.io.IOException;

import nez.Grammar;
import nez.Parser;
import nez.Strategy;
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
	}

	static Parser lParser;

	@Override
	public Parser getLoaderParser(String start) {
		if (lParser == null) {
			try {
				Strategy option = Strategy.newSafeStrategy();
				Grammar g = GrammarFileLoader.loadGrammar("regex.nez", option);
				// g.dump();
				lParser = g.newParser(option);
				strategy.report();
			} catch (IOException e) {
				ConsoleUtils.exit(1, "unload: " + e.getMessage());
			}
			assert(lParser != null);
		}
		return lParser;
	}

	@Override
	public void parse(Tree<?> node) {
		Production p = this.getGrammar().newProduction("Start", null);
		p.setExpression(pi(node, null));
	}

	final Expression pi(Tree<?> expr, Expression k) {
		return (Expression) visit("pi", Expression.class, expr, k);
	}

	public Expression piPattern(Tree<?> e, Expression k) {
		Expression notPattern = toSeq(null, ExpressionCommons.newPnot(null, pi(e.get(0), k)), toAny(null));
		Expression zeroMore = ExpressionCommons.newPzero(null, notPattern);
		Expression oneMore = ExpressionCommons.newPone(null, toSeq(null, pi(e.get(0), k), zeroMore));
		return toSeq(null, zeroMore, oneMore);
	}

	// pi(e, k) e: regular expression, k: continuation
	// pi(e1|e2, k) = pi(e1, k) / pi(e2, k)
	public Expression piOr(Tree<?> e, Expression k) {
		return toChoice(e, pi(e.get(0), k), pi(e.get(1), k));
	}

	// pi(e1e2, k) = pi(e1, pi(e2, k))
	public Expression piConcatenation(Tree<?> e, Expression k) {
		return pi(e.get(0), pi(e.get(1), k));
	}

	// pi((?>e), k) = pi(e, "") k
	public Expression piIndependentExpr(Tree<?> e, Expression k) {
		return toSeq(e, pi(e.get(0), toEmpty(e)), k);
	}

	// pi((?=e), k) = &pi(e, "") k
	public Expression piAnd(Tree<?> e, Expression k) {
		return toAnd(e, k);
	}

	// pi((?!e), k) = !pi(e, "") k
	public Expression piNot(Tree<?> e, Expression k) {
		return toNot(e, k);
	}

	// pi(e*+, k) = pi(e*, "") k
	public Expression piPossessiveRepetition(Tree<?> e, Expression k) {
		return toSeq(e, piRepetition(e, toEmpty(e)), k);
	}

	int NonTerminalCount = 0;

	// pi(e*?, k) = A, A <- k / pi(e, A)
	public Expression piLazyQuantifiers(Tree<?> e, Expression k) {
		String ruleName = "Repetition" + NonTerminalCount++;
		Expression ne = ExpressionCommons.newNonTerminal(e, this.getGrammar(), ruleName);
		if (k == null) {
			k = ExpressionCommons.newEmpty(null);
		}
		getGrammar().newProduction(ruleName, toChoice(e, k, pi(e.get(0), ne)));
		return ne;
	}

	// pi(e*, k) = A, A <- pi(e, A) / k
	public Expression piRepetition(Tree<?> e, Expression k) {
		String ruleName = "Repetition" + NonTerminalCount++;
		Expression ne = ExpressionCommons.newNonTerminal(e, this.getGrammar(), ruleName);
		getGrammar().newProduction(ruleName, toChoice(e, pi(e.get(0), ne), k));
		return ne;
	}

	// pi(e?, k) = pi(e, k) / k
	public Expression piOption(Tree<?> e, Expression k) {
		return toChoice(e, pi(e.get(0), k), k);
	}

	public Expression piOneMoreRepetition(Tree<?> e, Expression k) {
		return pi(e.get(0), piRepetition(e, k));
	}

	// pi(e{3}, k) = pi(e, pi(e, pi(e, k)))
	public Expression piNTimesRepetition(Tree<?> e, Expression k) {
		final int N = e.getInt(1, 0);
		Expression ne = k;
		for (int i = 0; i < N; i++) {
			ne = pi(e.get(0), ne);
		}
		return ne;
	}

	// pi(e{N,}, k) = pi(e{N}, A), A <- pi(e, A) / k
	public Expression piNMoreRepetition(Tree<?> e, Expression k) {
		return piNTimesRepetition(e, piRepetition(e, k));
	}

	// pi(e{N,M}, k) = pi(e{N}, A0), A0 <- pi(e, A1) / k, ... A(M-N) <- k
	public Expression piNtoMTimesRepetition(Tree<?> e, Expression k) {
		final int DIF = e.getInt(2, 0) - e.getInt(1, 0);
		String ruleName = "Repetition" + NonTerminalCount++;
		Expression ne = ExpressionCommons.newNonTerminal(e, this.getGrammar(), ruleName);
		getGrammar().newProduction(ruleName, k);
		for (int i = 0; i < DIF; i++) {
			ruleName = "Repetition" + NonTerminalCount++;
			Expression nne = ne;
			ne = ExpressionCommons.newNonTerminal(e, this.getGrammar(), ruleName);
			getGrammar().newProduction(ruleName, toChoice(e, pi(e.get(0), nne), k));
		}
		return piNTimesRepetition(e, ne);
	}

	public Expression piAny(Tree<?> e, Expression k) {
		return toSeq(e, k);
	}

	public Expression piCapture(Tree<?> e, Expression k) {
		return pi(e.get(0), k);
	}

	public Expression piNegativeCharacterSet(Tree<?> e, Expression k) {
		Expression nce = toSeq(e, ExpressionCommons.newPnot(e, toCharacterSet(e)), toAny(e));
		return toSeq(e, nce, k);
	}

	public Expression piCharacterSet(Tree<?> e, Expression k) {
		return toSeq(e, k);
	}

	public Expression piCharacterRange(Tree<?> e, Expression k) {
		return toSeq(e, k);
	}

	public Expression piCharacterSetItem(Tree<?> e, Expression k) {
		return toSeq(e, k);
	}

	// pi(c, k) = c k
	// c: single character
	public Expression piCharacter(Tree<?> c, Expression k) {
		return toSeq(c, k);
	}

	public Expression piHeader(Tree<?> e, Expression k) {
		return toSeq(e, k);
	}

	public Expression piTerminator(Tree<?> e, Expression k) {
		return toSeq(e, k);
	}

	private Expression toExpression(Tree<?> e) {
		return (Expression) this.visit("to", e);
	}

	public Expression toCharacter(Tree<?> c) {
		String text = c.toText();
		byte[] utf8 = StringUtils.toUtf8(text);
		if (utf8.length != 1) {
			ConsoleUtils.exit(1, "Error: not Character Literal");
		}
		return ExpressionCommons.newCbyte(null, false, utf8[0]);
	}

	boolean byteMap[];

	// boolean useByteMap = true;

	public Expression toCharacterSet(Tree<?> e) {
		UList<Expression> l = new UList<Expression>(new Expression[e.size()]);
		byteMap = new boolean[257];
		for (Tree<?> subnode : e) {
			ExpressionCommons.addChoice(l, toExpression(subnode));
		}
		return ExpressionCommons.newCset(null, false, byteMap);
	}

	public Expression toCharacterRange(Tree<?> e) {
		byte[] begin = StringUtils.toUtf8(e.get(0).toText());
		byte[] end = StringUtils.toUtf8(e.get(1).toText());
		for (byte i = begin[0]; i <= end[0]; i++) {
			byteMap[i] = true;
		}
		return ExpressionCommons.newCharSet(null, e.get(0).toText(), e.get(1).toText());
	}

	public Expression toCharacterSetItem(Tree<?> c) {
		byte[] utf8 = StringUtils.toUtf8(c.toText());
		byteMap[utf8[0]] = true;
		return ExpressionCommons.newCbyte(null, false, utf8[0]);
	}

	public Expression toEmpty(Tree<?> node) {
		return ExpressionCommons.newEmpty(null);
	}

	public Expression toAny(Tree<?> e) {
		return ExpressionCommons.newCany(null, false);
	}

	// stub
	// public Expression toHeader(Tree<?> e) {
	// }

	public Expression toTerminator(Tree<?> e) {
		return ExpressionCommons.newPnot(null, ExpressionCommons.newCany(null, false));
	}

	public Expression toAnd(Tree<?> e, Expression k) {
		return toSeq(e, ExpressionCommons.newPand(null, pi(e.get(0), toEmpty(e))), k);
	}

	public Expression toNot(Tree<?> e, Expression k) {
		return toSeq(e, ExpressionCommons.newPnot(null, pi(e.get(0), toEmpty(e))), k);
	}

	public Expression toChoice(Tree<?> node, Expression e, Expression k) {
		UList<Expression> l = new UList<Expression>(new Expression[2]);
		ExpressionCommons.addChoice(l, e);
		if (k != null) {
			ExpressionCommons.addChoice(l, k);
		} else {
			ExpressionCommons.addChoice(l, toEmpty(node));
		}
		return ExpressionCommons.newPchoice(null, l);
	}

	public Expression toSeq(Tree<?> e, Expression k) {
		UList<Expression> l = new UList<Expression>(new Expression[2]);
		ExpressionCommons.addSequence(l, toExpression(e));
		if (k != null) {
			ExpressionCommons.addSequence(l, k);
		}
		return ExpressionCommons.newPsequence(null, l);
	}

	public Expression toSeq(Tree<?> node, Expression e, Expression k) {
		UList<Expression> l = new UList<Expression>(new Expression[2]);
		ExpressionCommons.addSequence(l, e);
		if (k != null) {
			ExpressionCommons.addSequence(l, k);
		}
		return ExpressionCommons.newPsequence(null, l);
	}

}
