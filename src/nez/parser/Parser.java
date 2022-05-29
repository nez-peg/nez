package nez.parser;

import java.util.ArrayList;
import java.util.List;

import nez.ast.CommonTree;
import nez.ast.Source;
import nez.ast.SourceError;
import nez.ast.Tree;
import nez.lang.Grammar;
import nez.parser.io.CommonSource;
import nez.parser.vm.ParserMachineContext;
import nez.util.ConsoleUtils;
import nez.util.UList;

public final class Parser {
	private Grammar grammar;
	private ParserStrategy strategy;
	private String start;

	public Parser(Grammar grammar, String start, ParserStrategy strategy) {
		this.grammar = grammar;
		this.start = start;
		this.strategy = strategy;
	}

	public final Grammar getGrammar() {
		return grammar;
	}

	public final ParserStrategy getParserStrategy() {
		return this.strategy;
	}

	private Grammar compiledGrammar = null;
	private ParserCode<?> pcode = null;

	public final Grammar getCompiledGrammar() {
		if (compiledGrammar == null) {
			compiledGrammar = new ParserOptimizer().optimize(grammar.getProduction(start), strategy, null);
		}
		return compiledGrammar;
	}

	public final ParserCode<?> getParserCode() {
		if (this.pcode == null) {
			pcode = this.strategy.newParserCode(getCompiledGrammar());
		}
		return pcode;
	}

	public final ParserCode<?> compile() {
		this.pcode = this.strategy.newParserCode(getCompiledGrammar());
		return pcode;
	}

	public final ParserInstance newParserContext(Source source, Tree<?> prototype) {
		ParserCode<?> pcode = this.getParserCode();
		return this.strategy.newParserContext(source, pcode.getMemoPointSize(), prototype);
	}

	/* -------------------------------------------------------------------- */

	public final Object perform(ParserInstance context) {
		ParserCode<?> code = this.getParserCode();
		// context.init(newMemoTable(context), prototype);
		if (prof != null) {
			context.startProfiling(prof);
		}
		Object matched = code.exec(context);
		if (prof != null) {
			context.doneProfiling(prof);
		}
		if (matched == null) {
			perror(context.getSource(), context.getMaximumPosition(), "syntax error");
			return null;
		}
		if (this.disabledUncosumed && context.hasUnconsumed()) {
			perror(context.getSource(), context.getPosition(), "unconsumed");
		}
		return matched;
	}

	@SuppressWarnings("unchecked")
	public final <T extends Tree<T>> T perform(Source s, T proto) {
		if (strategy.Moz) {
			// Verbose.println("ClassicMoz");
			return (T) perform(this.newParserContext(s, proto));
		}
		// Verbose.println("FT86");
		ParserMachineContext<T> ctx = new ParserMachineContext<T>(s, proto);
		ParserCode<?> code = this.getParserCode();
		ctx.initMemoTable(strategy.SlidingWindow, code.getMemoPointSize());
		// if (prof != null) {
		// context.startProfiling(prof);
		// }
		T matched = code.exec(ctx);
		// if (prof != null) {
		// context.doneProfiling(prof);
		// }
		if (matched == null || (this.disabledUncosumed && !ctx.eof())) {
			perror(s, ctx.getMaximumPosition(), "syntax error");
			return null;
		}
		if (this.disabledUncosumed && !ctx.eof()) {
			perror(s, ctx.getPosition(), "unconsumed");
		}
		return matched;
	}

	protected ParserProfiler prof = null;

	public void setProfiler(ParserProfiler prof) {
		this.prof = prof;
		if (prof != null) {
			this.compile();
			// prof.setFile("G.File", this.start.getGrammarFile().getURN());
			prof.setCount("G.Production", this.grammar.size());
			prof.setCount("G.Instruction", this.pcode.getInstructionSize());
			prof.setCount("G.MemoPoint", this.pcode.getMemoPointSize());
		}
	}

	public ParserProfiler getProfiler() {
		return this.prof;
	}

	public void logProfiler() {
		if (prof != null) {
			prof.log();
		}
	}

	/* --------------------------------------------------------------------- */

	public final boolean match(Source s) {
		return perform(this.newParserContext(s, null)) != null;
	}

	public final boolean match(String str) {
		return match(CommonSource.newStringSource(str));
	}

	public <T extends Tree<T>> T parse(Source source, T proto) {
		return this.perform(source, proto);
	}

	public final CommonTree parse(Source sc) {
		return this.parse(sc, new CommonTree());
	}

	public final CommonTree parse(String str) {
		Source sc = CommonSource.newStringSource(str);
		return this.parse(sc, new CommonTree());
	}

	/* Errors */

	private boolean disabledUncosumed = false;
	private UList<SourceError> errors = null;

	public final void setDisabledUnconsumed(boolean disabled) {
		this.disabledUncosumed = disabled;
	}

	private void perror(Source source, long pos, String message) {
		if (this.errors == null) {
			this.errors = new UList<SourceError>(new SourceError[4]);
		}
		errors.add(new SourceError(source, pos, message));
	}

	public final boolean hasErrors() {
		return errors != null;
	}

	public final void clearErrors() {
		errors = null;
	}

	public final List<SourceError> getErrors() {
		return errors == null ? new ArrayList<SourceError>() : this.errors;
	}

	public final boolean showErrors() {
		if (errors != null) {
			for (SourceError e : errors) {
				ConsoleUtils.println(e.toString());
			}
			this.clearErrors();
			return true;
		}
		return false;
	}

	public final void ensureNoErrors() throws ParserException {
		if (errors != null) {
			throw new ParserException(errors.ArrayValues[0].toString());
		}
	}

}
