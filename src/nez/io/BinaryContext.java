package nez.io;

import java.io.UnsupportedEncodingException;

import nez.SourceContext;
import nez.ast.Source;
import nez.util.StringUtils;

public class BinaryContext extends SourceContext {
	private byte[] bin;
	long binLength;

	public BinaryContext(byte[] bin) {
		this("(binary)", 1, bin);
	}

	public BinaryContext(String resource, long linenum, byte[] bin) {
		super(resource, linenum);
		this.bin = bin;
		this.binLength = bin.length;
	}

	@Override
	public final int EOF() {
		return Source.BinaryEOF;
	}

	@Override
	public final int byteAt(long pos) {
		if(pos < binLength) {
			return this.bin[(int)pos] & 0xff;
		}
		return Source.BinaryEOF;
	}

	@Override
	public final long length() {
		return this.binLength;
	}

	@Override
	public final byte[] subbyte(long startIndex, long endIndex) {
		byte[] b = new byte[(int)(endIndex - startIndex)];
		System.arraycopy(this.bin, (int)(startIndex), b, 0, b.length);
		return b;
	}

	@Override
	public final String substring(long startIndex, long endIndex) {
		try {
			return new String(this.bin, (int)(startIndex), (int)(endIndex - startIndex), StringUtils.DefaultEncoding);
		}
		catch(UnsupportedEncodingException e) {
		}
		return null;
	}

	@Override
	public final long linenum(long pos) {
		long count = this.startLineNum;
		int end = (int)pos;
		if(end >= this.bin.length) {
			end = this.bin.length;
		}
		for(int i = 0; i < end; i++) {
			if(this.bin[i] == '\n') {
				count++;
			}
		}
		return count;
	}

	@Override
	public final boolean match(long pos, byte[] text) {
		if(pos + text.length > this.binLength) {
			return false;
		}
		for(int i = 0; i < text.length; i++) {
			if(text[i] != this.bin[(int)pos + i]) {
				return false;
			}
		}
		return true;
	}
}
