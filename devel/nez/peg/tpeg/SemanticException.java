package nez.peg.tpeg;

import java.util.Objects;

import nez.peg.tpeg.type.TypeException;

public class SemanticException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1629183207999691500L;

	private final LongRange range;

	/**
	 *
	 * @param range
	 *            not null
	 * @param message
	 */
	public SemanticException(LongRange range, String message) {
		super(message);
		this.range = Objects.requireNonNull(range);
	}

	public SemanticException(LongRange range, TypeException e) {
		super(e.getMessage(), e);
		this.range = Objects.requireNonNull(range);
	}

	public LongRange getRange() {
		return range;
	}

	public static void semanticError(LongRange range, String message) {
		throw new SemanticException(range, message);
	}
}
