package nez.lang.ast;

import nez.ast.SourcePosition;
import nez.ast.Tree;
import nez.lang.Grammar;
import nez.parser.ParserStrategy;
import nez.util.VisitorMap;

public class GrammarVisitorMap<T> extends VisitorMap<T> {
	protected final Grammar grammar;
	protected final ParserStrategy strategy;

	public GrammarVisitorMap(Grammar grammar, ParserStrategy strategy) {
		this.grammar = grammar;
		this.strategy = strategy;
	}

	public Grammar getGrammar() {
		return grammar;
	}

	public ParserStrategy getStrategy() {
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
