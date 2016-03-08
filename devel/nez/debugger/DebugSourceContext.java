package nez.debugger;

import java.io.File;
import java.io.IOException;

import nez.ast.Source;
import nez.parser.io.StringSource;
import nez.util.ConsoleUtils;

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
	public abstract String subString(long startIndex, long endIndex);

	@Override
	public abstract long linenum(long pos);

	/* handling input stream */

	@Override
	public final String getResourceName() {
		return fileName;
	}

	@Override
	public Source subSource(long startIndex, long endIndex) {
		return new StringSource(this.getResourceName(), this.linenum(startIndex), subByte(startIndex, endIndex), false);
	}

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
		return "(" + this.getResourceName() + ":" + this.linenum(pos) + ") [" + messageType + "] " + message;
	}

	@Override
	public final String formatPositionLine(String messageType, long pos, String message) {
		return this.formatPositionMessage(messageType, pos, message) + this.getTextAround(pos, "\n ");
	}

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
		while (this.byteAt(pos) == 0 && pos > 0) {
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
			while ((ch = byteAt(endIndex)) != 0) {
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
		while (this.byteAt(pos) == 0 && pos > 0) {
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
			while ((ch = byteAt(endIndex)) != 0) {
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
