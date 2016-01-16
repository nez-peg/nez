package nez.parser;

import nez.ast.Source;

public final class ParserInstance {
	Source source;
	ParserRuntime runtime;

	public ParserInstance(Source source, ParserRuntime runtime) {
		this.runtime = runtime;
		this.source = source;
	}

	public final Source getSource() {
		return this.source;
	}

	public final ParserRuntime getRuntime() {
		return this.runtime;
	}

	public boolean hasUnconsumed() {
		return this.runtime.hasUnconsumed();
	}

	public final long getPosition() {
		return this.runtime.getPosition();
	}

	public final long getMaximumPosition() {
		return this.runtime.getMaximumPosition();
	}

	public final String getErrorMessage(String errorType, String message) {
		return source.formatPositionLine(errorType, this.runtime.getMaximumPosition(), message);
	}

	public final String getSyntaxErrorMessage() {
		return source.formatPositionLine("error", this.getMaximumPosition(), "syntax error");
	}

	public final String getUnconsumedMessage() {
		return source.formatPositionLine("unconsumed", this.getPosition(), "");
	}

	public String getResourceName() {
		return source.getResourceName();
	}

	public void startProfiling(ParserProfier prof) {
		// TODO Auto-generated method stub

	}

	public void doneProfiling(ParserProfier prof) {
		// TODO Auto-generated method stub

	}

}
