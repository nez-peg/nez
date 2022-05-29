package nez.parser.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import nez.ast.Source;
import nez.util.FileBuilder;

public abstract class CommonSource implements Source {

	private String resourceName;
	protected long startLineNum = 1;

	protected CommonSource(String resourceName, long linenum) {
		this.resourceName = resourceName;
		this.startLineNum = linenum;
	}

	@Override
	public final String getResourceName() {
		return resourceName;
	}

	@Override
	public abstract long length();

	@Override
	public abstract int byteAt(long pos);

	@Override
	public abstract boolean eof(long pos);

	@Override
	public abstract boolean match(long pos, byte[] text);

	@Override
	public abstract String subString(long startIndex, long endIndex);

	@Override
	public Source subSource(long startIndex, long endIndex) {
		return new StringSource(this.getResourceName(), this.linenum(startIndex), subByte(startIndex, endIndex), false);
	}

	@Override
	public abstract long linenum(long pos);

	@Override
	public final int column(long pos) {
		int count = 0;
		for (long p = pos-1; p >= 0; p--, ++count) {
			if (this.byteAt(p) == '\n') {
				break;
			}
		}
		return count;
	}

	/* handling input stream */

	// final String getFilePath(String fileName) {
	// int loc = this.getResourceName().lastIndexOf("/");
	// if(loc > 0) {
	// return this.getResourceName().substring(0, loc+1) + fileName;
	// }
	// return fileName;
	// }

	private final long getLineStartPosition(long fromPostion) {
		long startIndex = fromPostion;
		if (!(startIndex < this.length())) {
			startIndex = this.length() - 1;
		}
		if (startIndex < 0) {
			startIndex = 0;
		}
		while (startIndex > 0) {
			int ch = byteAt(startIndex);
			if (ch == '\n') {
				startIndex = startIndex + 1;
				break;
			}
			startIndex = startIndex - 1;
		}
		return startIndex;
	}

	public final String getIndentText(long fromPosition) {
		long startPosition = this.getLineStartPosition(fromPosition);
		long i = startPosition;
		String indent = "";
		for (; i < fromPosition; i++) {
			int ch = this.byteAt(i);
			if (ch != ' ' && ch != '\t') {
				if (i + 1 != fromPosition) {
					for (long j = i; j < fromPosition; j++) {
						indent = indent + " ";
					}
				}
				break;
			}
		}
		indent = this.subString(startPosition, i) + indent;
		return indent;
	}

	public final String formatPositionMessage(String messageType, long pos, String message) {
		//return FileBuilder.extractFileName(getResourceName()) + ":" + linenum(pos) + ":" + column(pos) + " [" + messageType + "] " + message;
		return getResourceName() + ":" + linenum(pos) + ":" + column(pos) + " [" + messageType + "] " + message;
	}

	@Override
	public final String formatPositionLine(String messageType, long pos, String message) {
		return this.formatPositionMessage(messageType, pos, message) + this.getTextAround(pos, "\n ");
	}

	private final String getTextAround(long pos, String delim) {
		int ch = 0;
		if (pos < 0) {
			pos = 0;
		}
		while ((this.eof(pos) || this.byteAt(pos) == 0) && pos > 0) {
			pos -= 1;
		}
		long startIndex = pos;
		while (startIndex > 0) {
			ch = byteAt(startIndex);
			if (ch == '\n' && pos - startIndex > 0) {
				startIndex = startIndex + 1;
				break;
			}
			if (pos - startIndex > 60 && ch < 128) {
				break;
			}
			startIndex = startIndex - 1;
		}
		long endIndex = pos + 1;
		if (endIndex < this.length()) {
			while ((ch = byteAt(endIndex)) != 0 /* this.EOF() */) {
				if (ch == '\n' || endIndex - startIndex > 78 && ch < 128) {
					break;
				}
				endIndex = endIndex + 1;
			}
		} else {
			endIndex = this.length();
		}
		StringBuilder source = new StringBuilder();
		StringBuilder marker = new StringBuilder();
		for (long i = startIndex; i < endIndex; i++) {
			ch = byteAt(i);
			if (ch == '\n') {
				source.append("\\n");
				if (i == pos) {
					marker.append("^^");
				} else {
					marker.append("\\n");
				}
			} else if (ch == '\t') {
				source.append("    ");
				if (i == pos) {
					marker.append("^^^^");
				} else {
					marker.append("    ");
				}
			} else {
				source.append((char) ch);
				if (i == pos) {
					marker.append("^");
				} else {
					marker.append(" ");
				}
			}
		}
		return delim + source.toString() + delim + marker.toString();
	}

	public final static Source newStringSource(String str) {
		return new StringSource(str);
	}

	public final static Source newStringSource(String resource, long linenum, String str) {
		return new StringSource(resource, linenum, str);
	}

	public final static Source newFileSource(String fileName) throws IOException {
		File f = new File(fileName);
		if (!f.isFile()) {
			InputStream Stream = CommonSource.class.getResourceAsStream("/nez/lib/" + fileName);
			if (Stream != null) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(Stream));
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
				return new StringSource(fileName, 1, builder.toString());
			}
		}
		return new FileSource(fileName);
	}
}
