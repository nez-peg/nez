package nez.parser;

import java.util.ArrayList;
import java.util.List;

import nez.ast.CommonTree;
import nez.ast.Source;
import nez.ast.SourceError;
import nez.ast.Tree;
import nez.io.CommonSource;
import nez.lang.Grammar;
import nez.parser.moz.ParserGrammar;
import nez.util.ConsoleUtils;
import nez.util.UList;

public final class Parser {
	private ParserStrategy strategy;
	private ParserGrammar grammar;
	private ParserCode<?> pcode = null;

	public Parser(ParserGrammar pgrammar, ParserStrategy strategy) {
		this.grammar = pgrammar;
		this.strategy = strategy;
	}

	public final Grammar getParserGrammar() {
		return grammar;
	}

	public final ParserStrategy getParserStrategy() {
		return this.strategy;
	}

	public final ParserCode<?> compile() {
		this.pcode = this.strategy.newParserCode(grammar);
		return pcode;
	}

	public final ParserCode<?> getParserCode() {
		if (this.pcode == null) {
			pcode = this.strategy.newParserCode(grammar);
		}
		return pcode;
	}

	public final ParserContext newParserContext(Source source, Tree<?> prototype) {
		ParserCode<?> pcode = this.getParserCode();
		return this.strategy.newParserContext(source, pcode.getMemoPointSize(), prototype);
	}

	/* -------------------------------------------------------------------- */

	public final Object perform(ParserContext context) {
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

	protected ParserProfier prof = null;

	public void setProfiler(ParserProfier prof) {
		this.prof = prof;
		if (prof != null) {
			this.compile();
			// prof.setFile("G.File", this.start.getGrammarFile().getURN());
			prof.setCount("G.Production", this.grammar.size());
			prof.setCount("G.Instruction", this.pcode.getInstSize());
			prof.setCount("G.MemoPoint", this.pcode.getMemoPointSize());
		}
	}

	public ParserProfier getProfiler() {
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

	public Tree<?> parse(Source source, Tree<?> proto) {
		ParserContext context = this.newParserContext(source, proto);
		return (Tree<?>) this.perform(context);
	}

	public final CommonTree parse(Source sc) {
		return (CommonTree) this.parse(sc, new CommonTree());
	}

	public final CommonTree parse(String str) {
		Source sc = CommonSource.newStringSource(str);
		return (CommonTree) this.parse(sc, new CommonTree());
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
