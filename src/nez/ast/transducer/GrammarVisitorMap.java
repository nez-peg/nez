package nez.ast.transducer;

import nez.Grammar;
import nez.Strategy;
import nez.ast.SourcePosition;
import nez.ast.Tree;
import nez.util.VisitorMap;

public class GrammarVisitorMap<T> extends VisitorMap<T> {
	protected final Grammar grammar;
	protected final Strategy strategy;

	public GrammarVisitorMap(Grammar grammar, Strategy strategy) {
		this.grammar = grammar;
		this.strategy = strategy;
	}

	public Grammar getGrammar() {
		return grammar;
	}

	public Strategy getStrategy() {
		return strategy;
	}

	public final String key(Tree<?> node) {
		return node.getTag().getSymbol();
	}

	public final void reportError(SourcePosition s, String fmt, Object... args) {
		if (this.strategy != null) {
			this.strategy.reportError(s, fmt, args);
		}
	}

	public final void reportWarning(SourcePosition s, String fmt, Object... args) {
		if (this.strategy != null) {
			this.strategy.reportWarning(s, fmt, args);
		}
	}

	public final void reportNotice(SourcePosition s, String fmt, Object... args) {
		if (this.strategy != null) {
			this.strategy.reportNotice(s, fmt, args);
		}
	}

}
