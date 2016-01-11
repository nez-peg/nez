package nez.ast;

public interface Source {
	// public final static int BinaryEOF = 256;

	String getResourceName();

	long length();

	int byteAt(long pos);

	boolean eof(long pos);

	boolean match(long pos, byte[] text);

	String subString(long startIndex, long endIndex);

	byte[] subByte(long startIndex, long endIndex);

	Source subSource(long startIndex, long endIndex);

	long linenum(long pos);

	int column(long pos);

	String formatPositionLine(String messageType, long pos, String message);

	// public String formatDebugPositionMessage(long pos, String message);

}
