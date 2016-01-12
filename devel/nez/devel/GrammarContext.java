package nez.devel;

import java.util.TreeMap;

import nez.lang.Conditions;
import nez.lang.Expression;
import nez.lang.Grammar;
import nez.lang.Production;
import nez.lang.Typestate;
import nez.parser.ParserStrategy;

public class GrammarContext {
	Production start;
	TreeMap<String, Boolean> conditionMap;
	ParserStrategy strategy;

	GrammarContext(Production start, TreeMap<String, Boolean> conditionMap, ParserStrategy strategy) {
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

	private final Typestate.TypestateAnalyzer typestate = Typestate.newAnalyzer();

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

}
