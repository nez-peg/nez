package nez.ast;

public interface SourcePosition {
	public String formatSourceMessage(String type, String msg);
	public String formatDebugSourceMessage(String msg);
}
