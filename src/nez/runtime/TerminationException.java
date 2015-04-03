package nez.runtime;

@SuppressWarnings("serial")
public class TerminationException extends Exception {
	boolean status;
	TerminationException(boolean status) {
		super();
		this.status = status;
	}
}
