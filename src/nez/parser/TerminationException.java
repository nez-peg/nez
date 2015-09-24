package nez.parser;

@SuppressWarnings("serial")
public class TerminationException extends Exception {
	boolean status;

	public TerminationException(boolean status) {
		super();
		this.status = status;
	}
}
