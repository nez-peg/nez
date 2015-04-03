package nez.peg.regex;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import nez.Grammar;
import nez.NezException;
import nez.Production;
import nez.SourceContext;
import nez.ast.CommonTree;
import nez.ast.CommonTreeVisitor;
import nez.ast.Tag;
import nez.expr.Expression;
import nez.expr.Factory;
import nez.expr.GrammarChecker;
import nez.main.Verbose;
import nez.util.ConsoleUtils;
import nez.util.StringUtils;
import nez.util.UList;

public class RegexGrammar extends CommonTreeVisitor {

	static Grammar regexGrammar = null;
	public final static Grammar loadGrammar(SourceContext regex, GrammarChecker checker) throws IOException {
		if(regexGrammar == null) {
			try {
				regexGrammar = Grammar.loadGrammar("regex.nez");
			}
			catch(IOException e) {
				ConsoleUtils.exit(1, "can't load regex.nez");
			}
		}
		Production p = regexGrammar.newProduction("File");
		CommonTree node = p.parse(regex);
		if (node == null) {
			throw new NezException(regex.getSyntaxErrorMessage());
		}
		if (regex.hasUnconsumed()) {
			throw new NezException(regex.getUnconsumedMessage());
		}
		RegexGrammar conv = new RegexGrammar();
		Grammar grammar = new Grammar("re");
		conv.convert(node, grammar);
		checker.verify(grammar);
		return grammar;
	}
	
	public final static Production newProduction(String pattern) {
		try {
			Grammar grammar = loadGrammar(SourceContext.newStringContext(pattern), new GrammarChecker());
			return grammar.newProduction("File");
		} catch (IOException e) {
			Verbose.traceException(e);
		}
		return null;
	}

	public RegexGrammar() {
	}
	
	private Grammar grammar;

	public void convert(CommonTree e, Grammar grammar) {
		this.grammar = grammar;
		grammar.defineRule(e, "File", pi(e, null));
		grammar.defineRule(e, "Chunk", grammar.newNonTerminal("File"));
	}
	
	protected Method getClassMethod(String method, Tag tag) throws NoSuchMethodException, SecurityException {
		String name = method + tag.getName();
		return this.getClass().getMethod(name, CommonTree.class, Expression.class);
	}

	public final Expression pi(CommonTree expr, Expression k) {
		Tag tag = expr.getTag();
		Method m = findMethod("pi", tag);
		if(m != null) {
			try {
				return (Expression)m.invoke(this, expr, k);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				System.err.println(expr);
				e.printStackTrace();
			}
		}
		return null;
	}
		
	public Expression piPattern(CommonTree e, Expression k) {
		return this.pi(e.get(0), k);
	}

	// pi(e, k) e: regular expression, k: continuation
	// pi(e1|e2, k) = pi(e1, k) / pi(e2, k)
	public Expression piOr(CommonTree e, Expression k) {
		return toChoice(e, pi(e.get(0), k), pi(e.get(1), k));
	}

	// pi(e1e2, k) = pi(e1, pi(e2, k))
	public Expression piConcatenation(CommonTree e, Expression k) {
		return pi(e.get(0), pi(e.get(1), k));
	}

	// pi((?>e), k) = pi(e, "") k
	public Expression piIndependentExpr(CommonTree e, Expression k) {
		return toSeq(e, pi(e.get(0), toEmpty(e)), k);
	}

	// pi((?=e), k) = &pi(e, "") k
	public Expression piAnd(CommonTree e, Expression k) {
		return toAnd(e, k);
	}

	// pi((?!e), k) = !pi(e, "") k
	public Expression piNot(CommonTree e, Expression k) {
		return toNot(e, k);
	}

	// pi(e*+, k) = pi(e*, "") k
	public Expression piPossessiveRepetition(CommonTree e, Expression k) {
		return toSeq(e, piRepetition(e, toEmpty(e)), k);
	}

	int NonTerminalCount = 0;

	// pi(e*?, k) = A, A <- k / pi(e, A)
	public Expression piLazyQuantifiers(CommonTree e, Expression k) {
		String ruleName = "Repetition" + NonTerminalCount++;
		Expression ne = Factory.newNonTerminal(e, this.grammar, ruleName);
		grammar.defineRule(e, ruleName, toChoice(e, k, pi(e.get(0), ne)));
		return ne;
	}

	// pi(e*, k) = A, A <- pi(e, A) / k
	public Expression piRepetition(CommonTree e, Expression k) {
		String ruleName = "Repetition" + NonTerminalCount++;
		Expression ne = Factory.newNonTerminal(e, this.grammar, ruleName);
		grammar.defineRule(e, ruleName, toChoice(e, pi(e.get(0), ne), k));
		return ne;
	}
	
	// pi(e?, k) = pi(e, k) / k
	public Expression piOption(CommonTree e, Expression k) {
		return toChoice(e, pi(e.get(0), k), k);
	}

	public Expression piOneMoreRepetition(CommonTree e, Expression k) {
		return pi(e.get(0), piRepetition(e, k));
	}

	public Expression piAny(CommonTree e, Expression k) {
		return toAny(e);
	}

	public Expression piNegativeCharacterSet(CommonTree e, Expression k) {
		Expression nce = toSeq(e, Factory.newNot(e, toCharacterSet(e)), toAny(e));
		return toSeq(e, nce, k);
	}

	public Expression piCharacterSet(CommonTree e, Expression k) {
		return toSeq(e, k);
	}

	public Expression piCharacterRange(CommonTree e, Expression k) {
		return toSeq(e, k);
	}
	
	public Expression piCharacterSetItem(CommonTree e, Expression k) {
		return toSeq(e, k);
	}

	// pi(c, k) = c k
	// c: single character
	public Expression piCharacter(CommonTree c, Expression k) {
		return toSeq(c, k);
	}
	
	private Expression toExpression(CommonTree e) {
		return (Expression)this.visit("to", e);
	}
	
	public Expression toCharacter(CommonTree c) {
		String text = c.getText();
		byte[] utf8 = StringUtils.toUtf8(text);
		if (utf8.length !=1) {
			ConsoleUtils.exit(1, "Error: not Character Literal");
		}
		return Factory.newByteChar(null, utf8[0]);
	}
	
	boolean byteMap[];
	boolean useByteMap = true;
	public Expression toCharacterSet(CommonTree e) {
		UList<Expression> l = new UList<Expression>(new Expression[e.size()]);
		byteMap = new boolean[257];
		for(CommonTree subnode: e) {
			Factory.addChoice(l, toExpression(subnode));
		}
		if (useByteMap) {
			return Factory.newByteMap(null, byteMap);
		}
		else {
			return Factory.newChoice(null, l);
		}
	}
	
	public Expression toCharacterRange(CommonTree e) {
		byte[] begin = StringUtils.toUtf8(e.get(0).getText());
		byte[] end = StringUtils.toUtf8(e.get(1).getText());
		for(byte i = begin[0]; i <= end[0]; i++) {
			byteMap[i] = true;
		}
		return Factory.newCharSet(null, e.get(0).getText(), e.get(1).getText());
	}
	
	public Expression toCharacterSetItem(CommonTree c) {
		byte[] utf8 = StringUtils.toUtf8(c.getText());
		byteMap[utf8[0]] = true;
		return Factory.newByteChar(null, utf8[0]);
	}
	
	public Expression toEmpty(CommonTree node) {
		return Factory.newEmpty(null);
	}

	public Expression toAny(CommonTree e) {
		return Factory.newAnyChar(null);
	}
	
	public Expression toAnd(CommonTree e, Expression k) {
		return toSeq(e, Factory.newAnd(null, pi(e.get(0), toEmpty(e))), k);
	}
	
	public Expression toNot(CommonTree e, Expression k) {
		return toSeq(e, Factory.newNot(null, pi(e.get(0), toEmpty(e))), k);
	}

	public Expression toChoice(CommonTree node, Expression e, Expression k) {
		UList<Expression> l = new UList<Expression>(new Expression[2]);
		Factory.addChoice(l, e);
		if (k != null) {
			Factory.addChoice(l, k);
		}
		else {
			Factory.addChoice(l, toEmpty(node));
		}
		return Factory.newDirectChoice(null, l);
	}

	public Expression toSeq(CommonTree e, Expression k) {
		UList<Expression> l = new UList<Expression>(new Expression[2]);
		Factory.addSequence(l, toExpression(e));
		if(k != null) {
			Factory.addSequence(l, k);
		}
		return Factory.newSequence(null, l);
	}
	
	public Expression toSeq(CommonTree node, Expression e, Expression k) {
		UList<Expression> l = new UList<Expression>(new Expression[2]);
		Factory.addSequence(l, e);
		if (k != null) {
			Factory.addSequence(l, k);
		}
		return Factory.newSequence(null, l);
	}

}
