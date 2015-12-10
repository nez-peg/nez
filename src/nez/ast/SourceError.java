package nez.ast;

public class SourceError {
	final Source source;
	final long pos;
	final int level;
	final String message;

	public SourceError(Source source, long pos, int level, String message) {
		this.source = source;
		this.pos = pos;
		this.level = level;
		this.message = message;
	}

	public SourceError(Source source, long pos, String message) {
		this(source, pos, 0, message);
	}

	@Override
	public final String toString() {
		return source.formatPositionLine("error", pos, message);
	}
}
