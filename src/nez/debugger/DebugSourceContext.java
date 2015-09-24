package nez.debugger;

import java.io.File;
import java.io.IOException;

import nez.util.ConsoleUtils;
import nez.util.StringUtils;

public abstract class DebugSourceContext extends Context {

	private String fileName;
	protected long startLineNum = 1;

	protected DebugSourceContext(String fileName, long linenum) {
		this.fileName = fileName;
		this.startLineNum = linenum;
	}

	@Override
	public abstract int byteAt(long pos);

	@Override
	public abstract long length();

	@Override
	public abstract boolean match(long pos, byte[] text);

	@Override
	public abstract String substring(long startIndex, long endIndex);

	@Override
	public abstract long linenum(long pos);

	/* handling input stream */

	@Override
	public final String getResourceName() {
		return fileName;
	}

	// final String getFilePath(String fileName) {
	// int loc = this.getResourceName().lastIndexOf("/");
	// if(loc > 0) {
	// return this.getResourceName().substring(0, loc+1) + fileName;
	// }
	// return fileName;
	// }

	public final int charAt(long pos) {
		int c = byteAt(pos), c2, c3, c4;
		int len = StringUtils.lengthOfUtf8(c);
		switch (len) {
		case 1:
			return c;
		case 2:
			// 0b11111 = 31
			// 0b111111 = 63
			c2 = byteAt(pos + 1) & 63;
			return ((c & 31) << 6) | c2;
		case 3:
			c2 = byteAt(pos + 1) & 63;
			c3 = byteAt(pos + 2) & 63;
			return ((c & 15) << 12) | c2 << 6 | c3;
		case 4:
			c2 = byteAt(pos + 1) & 63;
			c3 = byteAt(pos + 2) & 63;
			c4 = byteAt(pos + 3) & 63;
			return ((c & 7) << 18) | c2 << 12 | c3 << 6 | c4;
		}
		return -1;
	}

	public final int charLength(long pos) {
		int c = byteAt(pos);
		return StringUtils.lengthOfUtf8(c);
	}

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
		indent = this.substring(startPosition, i) + indent;
		return indent;
	}

	public final String formatPositionMessage(String messageType, long pos, String message) {
		return "(" + this.getResourceName() + ":" + this.linenum(pos) + ") [" + messageType + "] " + message;
	}

	@Override
	public final String formatPositionLine(String messageType, long pos, String message) {
		return this.formatPositionMessage(messageType, pos, message) + this.getTextAround(pos, "\n ");
	}

	@Override
	public final String formatDebugPositionMessage(long pos, String message) {
		return "(" + this.getResourceName() + ":" + this.linenum(pos) + ")" + message;
	}

	// @Override
	// public final String formatDebugLine(long pos) {
	// return "(" + this.getResourceName() + ":" + this.linenum(pos) + ")" +
	// this.getTextLine(pos);
	// }
	// FIXME

	public final String formatDebugPositionLine(long pos, String message) {
		return this.formatDebugPositionMessage(pos, message) + this.getTextAround(pos, "\n ");
	}

	public final String getTextLine(long pos) {
		int ch = 0;
		if (pos < 0) {
			pos = 0;
		}
		while (this.byteAt(pos) == this.EOF() && pos > 0) {
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
			while ((ch = byteAt(endIndex)) != this.EOF()) {
				if (ch == '\n' || endIndex - startIndex > 78 && ch < 128) {
					break;
				}
				endIndex = endIndex + 1;
			}
		} else {
			endIndex = this.length();
		}
		StringBuilder source = new StringBuilder();
		for (long i = startIndex; i < endIndex; i++) {
			ch = byteAt(i);
			if (ch == '\t') {
				source.append("    ");
			} else {
				source.append((char) ch);
			}
		}
		return source.toString();
	}

	private final String getTextAround(long pos, String delim) {
		int ch = 0;
		if (pos < 0) {
			pos = 0;
		}
		while (this.byteAt(pos) == this.EOF() && pos > 0) {
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
			while ((ch = byteAt(endIndex)) != this.EOF()) {
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
				source.append("\\N");
				if (i == pos) {
					marker.append("^^");
				} else {
					marker.append("\\N");
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

	public final static DebugSourceContext newStringContext(String str) {
		return new DebugStringContext(str);
	}

	public final static DebugSourceContext newStringSourceContext(String resource, long linenum, String str) {
		return new DebugStringContext(resource, linenum, str);
	}

	public final static DebugSourceContext newDebugFileContext(String fileName) throws IOException {
		File f = new File(fileName);
		if (!f.isFile()) {
			ConsoleUtils.exit(1, "error: Input of Debugger is file");
		}
		return new DebugFileContext(fileName);
	}
}
