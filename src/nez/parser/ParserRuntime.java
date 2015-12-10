package nez.parser;

public abstract class ParserRuntime {

	public abstract boolean hasUnconsumed();

	public abstract long getPosition();

	public abstract long getMaximumPosition();

}
