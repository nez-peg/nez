package nez.lang;

import java.util.HashMap;
import java.util.TreeMap;

import nez.parser.ParserStrategy;

public class GrammarContext {
	Production start;
	TreeMap<String, Boolean> conditionMap;
	ParserStrategy strategy;

	public GrammarContext(Production start, TreeMap<String, Boolean> conditionMap, ParserStrategy strategy) {
		this.start = start;
		this.strategy = strategy;
		this.conditionMap = conditionMap;
	}

	public Grammar newGrammar() {
		return new Grammar();
	}

	public Production getStartProduction() {
		return this.start;
	}

	public ParserStrategy getParserStrategy() {
		return this.strategy;
	}

	public Conditions newConditions() {
		return Conditions.newConditions(start, conditionMap);
	}

	/* Contextual */

	/* Typestate */

	private final Typestate.Analyzer typestate = new Typestate.Analyzer();

	public final Typestate typeState(Production p) {
		return typestate.inferTypestate(p);
	}

	public final Typestate typeState(Expression e) {
		return typestate.inferTypestate(e);
	}

	// Report

	public final void reportError(Expression p, String message) {
		this.strategy.reportError(p.getSourceLocation(), message);
	}

	public final void reportWarning(Expression p, String message) {
		this.strategy.reportWarning(p.getSourceLocation(), message);
	}

	public final void reportNotice(Expression p, String message) {
		this.strategy.reportNotice(p.getSourceLocation(), message);
	}

	public final static HashMap<String, Integer> count(Grammar g) {
		HashMap<String, Integer> counts = new HashMap<>();
		for (Production p : g) {
			count(p.getExpression(), counts);
		}
		return counts;
	}

	public final static void count(Expression e, HashMap<String, Integer> counts) {
		if (e instanceof NonTerminal) {
			String uname = ((NonTerminal) e).getUniqueName();
			Integer n = counts.get(uname);
			if (n == null) {
				counts.put(uname, 1);
			} else {
				counts.put(uname, n + 1);
			}
			return;
		}
		for (Expression sub : e) {
			count(sub, counts);
		}
	}

}
