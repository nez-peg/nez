package nez.debugger;

@SuppressWarnings("serial")
public class MachineExitException extends Exception {
	boolean result;

	public MachineExitException(boolean result) {
		super();
		this.result = result;
	}
}
