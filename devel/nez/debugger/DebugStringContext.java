package nez.debugger;

import java.io.UnsupportedEncodingException;

import nez.util.StringUtils;

public class DebugStringContext extends DebugSourceContext {
	private byte[] utf8;
	long textLength;

	public DebugStringContext(String sourceText) {
		super("(string)", 1);
		this.utf8 = toZeroTerminalByteSequence(sourceText);
		this.textLength = utf8.length - 1;
	}

	public DebugStringContext(String resource, long linenum, String sourceText) {
		super(resource, linenum);
		this.utf8 = toZeroTerminalByteSequence(sourceText);
		this.textLength = utf8.length - 1;
	}

	private final byte[] toZeroTerminalByteSequence(String s) {
		byte[] b = StringUtils.utf8(s);
		byte[] b2 = new byte[b.length + 1];
		System.arraycopy(b, 0, b2, 0, b.length);
		return b2;
	}

	@Override
	public final long length() {
		return this.textLength;
	}

	@Override
	public final int byteAt(long pos) {
		return this.utf8[(int) pos] & 0xff;
	}

	@Override
	public final boolean eof(long pos) {
		return pos >= this.textLength;
	}

	@Override
	public final byte[] subByte(long startIndex, long endIndex) {
		byte[] b = new byte[(int) (endIndex - startIndex)];
		System.arraycopy(this.utf8, (int) (startIndex), b, 0, b.length);
		return b;
	}

	@Override
	public final String subString(long startIndex, long endIndex) {
		try {
			return new String(this.utf8, (int) (startIndex), (int) (endIndex - startIndex), StringUtils.DefaultEncoding);
		} catch (UnsupportedEncodingException e) {
		}
		return null;
	}

	@Override
	public final long linenum(long pos) {
		long count = this.startLineNum;
		int end = (int) pos;
		if (end >= this.utf8.length) {
			end = this.utf8.length;
		}
		for (int i = 0; i < end; i++) {
			if (this.utf8[i] == '\n') {
				count++;
			}
		}
		return count;
	}

	@Override
	public final boolean match(long pos, byte[] text) {
		if (pos + text.length > this.textLength) {
			return false;
		}
		for (int i = 0; i < text.length; i++) {
			if (text[i] != this.utf8[(int) pos + i]) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int column(long pos) {
		return 0;
	}

}