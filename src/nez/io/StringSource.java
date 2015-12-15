package nez.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import nez.ast.Source;
import nez.util.StringUtils;

public class StringSource extends CommonSource {
	private byte[] utf8;
	long textLength;

	public StringSource(String sourceText) {
		super("(string)", 1);
		this.utf8 = toZeroTerminalByteSequence(sourceText);
		this.textLength = utf8.length - 1;
	}

	public StringSource(String resource, long linenum, String sourceText) {
		super(resource, linenum);
		this.utf8 = toZeroTerminalByteSequence(sourceText);
		this.textLength = utf8.length - 1;
	}

	public StringSource(String resource, long linenum, byte[] buffer, boolean nullChar) {
		super(resource, linenum);
		if (nullChar) {
			this.utf8 = buffer;
			this.textLength = buffer.length - 1;
		} else {
			this.utf8 = new byte[buffer.length + 1];
			System.arraycopy(buffer, 0, this.utf8, 0, buffer.length);
		}
		this.textLength = utf8.length - 1;
	}

	private final byte[] toZeroTerminalByteSequence(String s) {
		byte[] b = StringUtils.toUtf8(s);
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
	public Source subSource(long startIndex, long endIndex) {
		byte[] b = new byte[(int) (endIndex - startIndex) + 1];
		System.arraycopy(this.utf8, (int) (startIndex), b, 0, b.length);
		return new StringSource(this.getResourceName(), this.linenum(startIndex), b, true);
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

	/* utils */

	public final static CommonSource loadClassPath(String fileName, String[] classPath) throws IOException {
		File f = new File(fileName);
		if (f.isFile()) {
			return loadStream(f.getAbsolutePath(), new FileInputStream(f));
		}
		for (String path : classPath) {
			path = "/" + path + "/" + fileName;
			InputStream stream = CommonSource.class.getResourceAsStream(path);
			if (stream != null) {
				return loadStream(path, stream);
			}
		}
		throw new FileNotFoundException(fileName);
	}

	private final static CommonSource loadStream(String urn, InputStream stream) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		StringBuilder builder = new StringBuilder();
		String line = reader.readLine();
		while (true) {
			builder.append(line);
			line = reader.readLine();
			if (line == null) {
				break;
			}
			builder.append("\n");
		}
		reader.close();
		return new StringSource(urn, 1, builder.toString());
	}

}