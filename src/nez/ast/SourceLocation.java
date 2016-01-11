package nez.ast;

public interface SourceLocation {
	Source getSource();

	long getSourcePosition();

	int getLineNum();

	int getColumn();

	String formatSourceMessage(String type, String msg);

}
