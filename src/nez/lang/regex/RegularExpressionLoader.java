package nez.lang.regex;

import java.io.IOException;

import nez.Grammar;
import nez.Parser;
import nez.Strategy;
import nez.ast.AbstractTree;
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
	public Parser getLoaderGrammar() {
		if (lParser == null) {
			try {
				Strategy option = Strategy.newSafeStrategy();
				Grammar g = GrammarFileLoader.loadGrammar("regex.nez", option, null);
				// g.dump();
				lParser = g.newParser(option, repo);
				if (repo != null) {
					repo.report(option);
				}
			} catch (IOException e) {
				ConsoleUtils.exit(1, "unload: " + e.getMessage());
			}
			assert (lParser != null);
		}
		return lParser;
	}

	@Override
	public void parse(AbstractTree<?> node) {
		Production p = this.getGrammar().newProduction("Start", null);
		p.setExpression(pi(node, null));
	}

	final Expression pi(AbstractTree<?> expr, Expression k) {
		return (Expression) visit("pi", Expression.class, expr, k);
	}

	public Expression piPattern(AbstractTree<?> e, Expression k) {
		return this.pi(e.get(0), k);
	}

	// pi(e, k) e: regular expression, k: continuation
	// pi(e1|e2, k) = pi(e1, k) / pi(e2, k)
	public Expression piOr(AbstractTree<?> e, Expression k) {
		return toChoice(e, pi(e.get(0), k), pi(e.get(1), k));
	}

	// pi(e1e2, k) = pi(e1, pi(e2, k))
	public Expression piConcatenation(AbstractTree<?> e, Expression k) {
		return pi(e.get(0), pi(e.get(1), k));
	}

	// pi((?>e), k) = pi(e, "") k
	public Expression piIndependentExpr(AbstractTree<?> e, Expression k) {
		return toSeq(e, pi(e.get(0), toEmpty(e)), k);
	}

	// pi((?=e), k) = &pi(e, "") k
	public Expression piAnd(AbstractTree<?> e, Expression k) {
		return toAnd(e, k);
	}

	// pi((?!e), k) = !pi(e, "") k
	public Expression piNot(AbstractTree<?> e, Expression k) {
		return toNot(e, k);
	}

	// pi(e*+, k) = pi(e*, "") k
	public Expression piPossessiveRepetition(AbstractTree<?> e, Expression k) {
		return toSeq(e, piRepetition(e, toEmpty(e)), k);
	}

	int NonTerminalCount = 0;

	// pi(e*?, k) = A, A <- k / pi(e, A)
	public Expression piLazyQuantifiers(AbstractTree<?> e, Expression k) {
		String ruleName = "Repetition" + NonTerminalCount++;
		Expression ne = ExpressionCommons.newNonTerminal(e, this.getGrammar(), ruleName);
		if (k == null) {
			k = ExpressionCommons.newEmpty(null);
		}
		getGrammar().newProduction(ruleName, toChoice(e, k, pi(e.get(0), ne)));
		return ne;
	}

	// pi(e*, k) = A, A <- pi(e, A) / k
	public Expression piRepetition(AbstractTree<?> e, Expression k) {
		String ruleName = "Repetition" + NonTerminalCount++;
		Expression ne = ExpressionCommons.newNonTerminal(e, this.getGrammar(), ruleName);
		getGrammar().newProduction(ruleName, toChoice(e, pi(e.get(0), ne), k));
		return ne;
	}

	// pi(e?, k) = pi(e, k) / k
	public Expression piOption(AbstractTree<?> e, Expression k) {
		return toChoice(e, pi(e.get(0), k), k);
	}

	public Expression piOneMoreRepetition(AbstractTree<?> e, Expression k) {
		return pi(e.get(0), piRepetition(e, k));
	}

	public Expression piAny(AbstractTree<?> e, Expression k) {
		return toSeq(e, k);
	}

	public Expression piNegativeCharacterSet(AbstractTree<?> e, Expression k) {
		Expression nce = toSeq(e, ExpressionCommons.newPnot(e, toCharacterSet(e)), toAny(e));
		return toSeq(e, nce, k);
	}

	public Expression piCharacterSet(AbstractTree<?> e, Expression k) {
		return toSeq(e, k);
	}

	public Expression piCharacterRange(AbstractTree<?> e, Expression k) {
		return toSeq(e, k);
	}

	public Expression piCharacterSetItem(AbstractTree<?> e, Expression k) {
		return toSeq(e, k);
	}

	// pi(c, k) = c k
	// c: single character
	public Expression piCharacter(AbstractTree<?> c, Expression k) {
		return toSeq(c, k);
	}

	private Expression toExpression(AbstractTree<?> e) {
		return (Expression) this.visit("to", e);
	}

	public Expression toCharacter(AbstractTree<?> c) {
		String text = c.toText();
		byte[] utf8 = StringUtils.toUtf8(text);
		if (utf8.length != 1) {
			ConsoleUtils.exit(1, "Error: not Character Literal");
		}
		return ExpressionCommons.newCbyte(null, false, utf8[0]);
	}

	boolean byteMap[];

	// boolean useByteMap = true;

	public Expression toCharacterSet(AbstractTree<?> e) {
		UList<Expression> l = new UList<Expression>(new Expression[e.size()]);
		byteMap = new boolean[257];
		for (AbstractTree<?> subnode : e) {
			ExpressionCommons.addChoice(l, toExpression(subnode));
		}
		return ExpressionCommons.newCset(null, false, byteMap);
	}

	public Expression toCharacterRange(AbstractTree<?> e) {
		byte[] begin = StringUtils.toUtf8(e.get(0).toText());
		byte[] end = StringUtils.toUtf8(e.get(1).toText());
		byteMap = new boolean[257];
		for (byte i = begin[0]; i <= end[0]; i++) {
			byteMap[i] = true;
		}
		return ExpressionCommons.newCharSet(null, e.get(0).toText(), e.get(1).toText());
	}

	public Expression toCharacterSetItem(AbstractTree<?> c) {
		byte[] utf8 = StringUtils.toUtf8(c.toText());
		byteMap[utf8[0]] = true;
		return ExpressionCommons.newCbyte(null, false, utf8[0]);
	}

	public Expression toEmpty(AbstractTree<?> node) {
		return ExpressionCommons.newEmpty(null);
	}

	public Expression toAny(AbstractTree<?> e) {
		return ExpressionCommons.newCany(null, false);
	}

	public Expression toAnd(AbstractTree<?> e, Expression k) {
		return toSeq(e, ExpressionCommons.newPand(null, pi(e.get(0), toEmpty(e))), k);
	}

	public Expression toNot(AbstractTree<?> e, Expression k) {
		return toSeq(e, ExpressionCommons.newPnot(null, pi(e.get(0), toEmpty(e))), k);
	}

	public Expression toChoice(AbstractTree<?> node, Expression e, Expression k) {
		UList<Expression> l = new UList<Expression>(new Expression[2]);
		ExpressionCommons.addChoice(l, e);
		if (k != null) {
			ExpressionCommons.addChoice(l, k);
		} else {
			ExpressionCommons.addChoice(l, toEmpty(node));
		}
		return ExpressionCommons.newPchoice(null, l);
	}

	public Expression toSeq(AbstractTree<?> e, Expression k) {
		UList<Expression> l = new UList<Expression>(new Expression[2]);
		ExpressionCommons.addSequence(l, toExpression(e));
		if (k != null) {
			ExpressionCommons.addSequence(l, k);
		}
		return ExpressionCommons.newPsequence(null, l);
	}

	public Expression toSeq(AbstractTree<?> node, Expression e, Expression k) {
		UList<Expression> l = new UList<Expression>(new Expression[2]);
		ExpressionCommons.addSequence(l, e);
		if (k != null) {
			ExpressionCommons.addSequence(l, k);
		}
		return ExpressionCommons.newPsequence(null, l);
	}

}
