package konoha.script;

@SuppressWarnings("serial")
public class ScriptRuntimeException extends RuntimeException {

	public ScriptRuntimeException(String msg) {
		super(msg);
	}

	public ScriptRuntimeException(String fmt, Object... args) {
		super(String.format(fmt, args));
	}
}
