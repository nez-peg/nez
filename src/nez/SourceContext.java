package nez;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import nez.io.FileSourceContext;
import nez.io.StringSourceContext;
import nez.runtime.Context;
import nez.util.StringUtils;

public abstract class SourceContext extends Context {
	
	private String     fileName;
	protected long     startLineNum = 1;

	protected SourceContext(String fileName, long linenum) {
		this.fileName = fileName;
		this.startLineNum = linenum;
	}
	
	@Override
	public abstract int     byteAt(long pos);
	@Override
	public abstract long    length();

	@Override
	public abstract boolean match(long pos, byte[] text);
	@Override
	public abstract String  substring(long startIndex, long endIndex);
	@Override
	public abstract long    linenum(long pos);

	/* handling input stream */
	
	@Override
	public final String getResourceName() {
		return fileName;
	}

//	final String getFilePath(String fileName) {
//		int loc = this.getResourceName().lastIndexOf("/");
//		if(loc > 0) {
//			return this.getResourceName().substring(0, loc+1) + fileName; 
//		}
//		return fileName;
//	}

	public final int charAt(long pos) {
		int c = byteAt(pos), c2, c3, c4;
		int len = StringUtils.lengthOfUtf8(c);
		switch(len) {
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
		if(!(startIndex < this.length())) {
			startIndex = this.length() - 1;
		}
		if(startIndex < 0) {
			startIndex = 0;
		}
		while(startIndex > 0) {
			int ch = byteAt(startIndex);
			if(ch == '\n') {
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
		for(; i < fromPosition; i++) {
			int ch = this.byteAt(i);
			if(ch != ' ' && ch != '\t') {
				if(i + 1 != fromPosition) {
					for(long j = i;j < fromPosition; j++) {
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
		return "(" + this.getResourceName() + ":" + this.linenum(pos) + ") [" + messageType +"] " + message;
	}

	@Override
	public final String formatPositionLine(String messageType, long pos, String message) {
		return this.formatPositionMessage(messageType, pos, message) + this.getTextAround(pos, "\n ");
	}

	private final String getTextAround(long pos, String delim) {
		int ch = 0;
		if(pos < 0) {
			pos = 0;
		}
		while(this.byteAt(pos) == this.EOF() && pos > 0) {
			pos -= 1;
		}
		long startIndex = pos;
		while(startIndex > 0) {
			ch = byteAt(startIndex);
			if(ch == '\n' && pos - startIndex > 0) {
				startIndex = startIndex + 1;
				break;
			}
			if(pos - startIndex > 60 && ch < 128) {
				break;
			}
			startIndex = startIndex - 1;
		}
		long endIndex = pos + 1;
		if(endIndex < this.length()) {
			while((ch = byteAt(endIndex)) != this.EOF()) {
				if(ch == '\n' || endIndex - startIndex > 78 && ch < 128) {
					break;
				}
				endIndex = endIndex + 1;
			}
		}
		else {
			endIndex = this.length();
		}
		StringBuilder source = new StringBuilder();
		StringBuilder marker = new StringBuilder();
		for(long i = startIndex; i < endIndex; i++) {
			ch = byteAt(i);
			if(ch == '\n') {
				source.append("\\N");
				if(i == pos) {
					marker.append("^^");
				}
				else {
					marker.append("\\N");
				}
			}
			else if(ch == '\t') {
				source.append("    ");
				if(i == pos) {
					marker.append("^^^^");
				}
				else {
					marker.append("    ");
				}
			}
			else {
				source.append((char)ch);
				if(i == pos) {
					marker.append("^");
				}
				else {
					marker.append(" ");
				}
			}
		}
		return delim + source.toString() + delim + marker.toString();
	}
	
	public final static SourceContext newStringSourceContext(String str) {
		return new StringSourceContext(str);
	}

	public final static SourceContext newStringSourceContext(String resource, long linenum, String str) {
		return new StringSourceContext(resource, linenum, str);
	}

	public final static SourceContext loadSource(String fileName) throws IOException {
		InputStream Stream = SourceContext.class.getResourceAsStream("/" + fileName);
		if (Stream == null) {
			File f = new File(fileName);
			if(f.length() > 16 * 1024) {
				return new FileSourceContext(fileName);
			}
			Stream = new FileInputStream(fileName);
		}
		BufferedReader reader = new BufferedReader(new InputStreamReader(Stream));
		StringBuilder builder = new StringBuilder();
		String line = reader.readLine();
		while(true) {
			builder.append(line);
			line = reader.readLine();
			if (line == null) {
				break;
			}
			builder.append("\n");
		}
		reader.close();
		return new StringSourceContext(fileName, 1, builder.toString());
	}
}


