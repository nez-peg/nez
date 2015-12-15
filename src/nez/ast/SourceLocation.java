package nez.ast;

public interface SourceLocation {
	public Source getSource();

	public long getSourcePosition();

	public int getLineNum();

	public int getColumn();

	public String formatSourceMessage(String type, String msg);

}
