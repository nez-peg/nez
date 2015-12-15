package nez.ast;

public interface Source {
	// public final static int BinaryEOF = 256;

	public String getResourceName();

	public long length();

	public int byteAt(long pos);

	public boolean eof(long pos);

	public boolean match(long pos, byte[] text);

	public String subString(long startIndex, long endIndex);

	public byte[] subByte(long startIndex, long endIndex);

	public Source subSource(long startIndex, long endIndex);

	public long linenum(long pos);

	public int column(long pos);

	public String formatPositionLine(String messageType, long pos, String message);

	// public String formatDebugPositionMessage(long pos, String message);

}
