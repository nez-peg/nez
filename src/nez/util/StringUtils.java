package nez.util;

import java.io.UnsupportedEncodingException;

import nez.expr.ByteMap;

public abstract class StringUtils {

	public final static String DefaultEncoding = "UTF8";

	private final static int E = 1;
	final static int[] utf8LengthMatrix = {
		1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
		1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
		1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
		1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
		1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
		1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
		1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
		1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
		E, E, E, E, E, E, E, E, E, E, E, E, E, E, E, E,
		E, E, E, E, E, E, E, E, E, E, E, E, E, E, E, E,
		E, E, E, E, E, E, E, E, E, E, E, E, E, E, E, E,
		E, E, E, E, E, E, E, E, E, E, E, E, E, E, E, E,
		E, E, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
		2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
		3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,
		4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, E, E,
		0 /* EOF */
	};

	public final static byte[] toUtf8(String text) {
		try {
			return text.getBytes(DefaultEncoding);
		} catch (UnsupportedEncodingException e) {
			ConsoleUtils.exit(1, "unsupported character: " + e);
		}
		return text.getBytes();
	}

	public final static int lengthOfUtf8(int ch) {
		return StringUtils.utf8LengthMatrix[ch];
	}

	public final static int lengthOfUtf8(byte ch) {
		return StringUtils.utf8LengthMatrix[ch & 0xff];
	}

	
	public final static String quoteString(char openChar, String text, char closeChar) {
		StringBuilder sb = new StringBuilder();
		StringUtils.formatQuoteString(sb, openChar, text, closeChar);
		return sb.toString();
	}

	public final static void formatQuoteString(StringBuilder sb, char openChar, String text, char closeChar) {
		char slashChar = '\\';
		sb.append(openChar);
		int i = 0;
		for(; i < text.length(); i = i + 1) {
			char ch = text.charAt(i);
			if(ch == '\n') {
				sb.append(slashChar);
				sb.append("n");
			}
			else if(ch == '\t') {
				sb.append(slashChar);
				sb.append("t");
			}
			else if(ch == closeChar) {
				sb.append(slashChar);
				sb.append(ch);
			}
			else if(ch == '\\') {
				sb.append(slashChar);
				sb.append(slashChar);
			}
			else {
				sb.append(ch);
			}
		}
		sb.append(closeChar);
	}

	public final static String unquoteString(String text) {
		if(text.indexOf("\\") == -1) {
			return text;
		}
		CharReader r = new CharReader(text);
		StringBuilder sb = new StringBuilder();
		while(r.hasChar()) {
			char ch = r.readChar();
			if(ch == '0') {
				break;
			}
			sb.append(ch);
		}
		return sb.toString();
	}

	// USED
	public final static int parseInt(String text, int defval) {
		if(text.length() > 0) {
			try {
				return Integer.parseInt(text);
			}
			catch(NumberFormatException e) {
				//e.printStackTrace();
			}
		}
		return defval;
	}

	// Used in Factory.newCharClass
	public final static int parseAscii(String t) {
		if(t.startsWith("\\x")) {
			int c = StringUtils.hex(t.charAt(2));
			c = (c * 16) + StringUtils.hex(t.charAt(3));
			return c;
		}
		if(t.startsWith("\\u")) {
			return -1;
		}
		if(t.startsWith("\\") && t.length() > 1) {
			int c = t.charAt(1);
			switch (c) {
//			case 'a':  return '\007'; /* bel */
//			case 'b':  return '\b';  /* bs */
//			case 'e':  return '\033'; /* esc */
			case 'f':  return '\f';   /* ff */
			case 'n':  return '\n';   /* nl */
			case 'r':  return '\r';   /* cr */
			case 't':  return '\t';   /* ht */
			case 'v':  return '\013'; /* vt */
			}
			return c;
		}
		return -1;
	}

	public final static int parseUnicode(String t) {
		if(t.startsWith("\\u")) {
			int c = StringUtils.hex(t.charAt(2));
			c = (c * 16) + StringUtils.hex(t.charAt(3));
			c = (c * 16) + StringUtils.hex(t.charAt(4));
			c = (c * 16) + StringUtils.hex(t.charAt(5));
			return c;
		}
		return t.charAt(0);
	}

	// USED
	public static int hex(int c) {
		if('0' <= c && c <= '9') {
			return c - '0';
		}
		if('a' <= c && c <= 'f') {
			return c - 'a' + 10;
		}
		if('A' <= c && c <= 'F') {
			return c - 'A' + 10;
		}
		return 0;
	}

	public static char parseHex4(char ch1, char ch2, char ch3, char ch4) {
		int c = StringUtils.hex(ch1);
		c = (c * 16) + StringUtils.hex(ch2);
		c = (c * 16) + StringUtils.hex(ch3);
		c = (c * 16) + StringUtils.hex(ch4);
		return (char)c;
	}

	
	public final static String stringfyByte(int ch) {
		char c = (char)ch;
		switch(c) {
		case '\n' : return("'\\n'"); 
		case '\t' : return("'\\t'"); 
		case '\r' : return("'\\r'"); 
		case '\'' : return("'\\''"); 
		case '\\' : return("'\\\\'"); 
		}
		if(Character.isISOControl(c) || c > 127) {
			return(String.format("0x%02x", (int)c));
		}
		return("'" + c + "'");
	}
	
	// The below are used in ByteMap
	
	public final static String stringfyCharClass(int startChar, int endChar) {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		appendCharClass(sb, startChar);
		sb.append("-");
		appendCharClass(sb, endChar);
		sb.append("]");
		return sb.toString();
	}
	
	public final static String stringfyCharClass(boolean[] b) {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for(int s = 0; s < 256; s++) {
			if(b[s]) {
				int e = searchEndChar(b, s+1);
				if(s == e) {
					appendCharClass(sb, s);
				}
				else {
					appendCharClass(sb, s);
					sb.append("-");
					appendCharClass(sb, e);
					s = e;
				}
			}
		}
		sb.append("]");
		return sb.toString();
	}

	private final static int searchEndChar(boolean[] b, int s) {
		for(; s < 256; s++) {
			if(!b[s]) {
				return s-1;
			}
		}
		return 255;
	}

	private static void appendCharClass(StringBuilder sb, int ch) {
		char c = (char)ch;
		switch(c) {
		case '\n' : sb.append("\\n"); break;
		case '\t' : sb.append("\\t"); break;
		case '\r' : sb.append("\\r"); break;
		case '\'' : sb.append("\\'"); break;
		case ']' : sb.append("\\]"); break;
		case '-' : sb.append("\\-"); break;
		case '\\' : sb.append("\\\\"); break;
		default:
			if(Character.isISOControl(c) || c > 127) {
				sb.append(String.format("\\x%02x", (int)c));
			}
			else {
				sb.append(c);
			}
		}
	}

	public final static String stringfyByteMap(boolean[] b) {
		StringBuilder sb = new StringBuilder();
		for(int offset = 0; offset < 127; offset += 4) {
			stringfyByteMapImpl(sb, b, offset);
		}
		return sb.toString();
	}

	private final static char[] HexChar = {
		'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
	};
	
	private static void stringfyByteMapImpl(StringBuilder sb, boolean[] b, int offset) {
		int n = 0;
		if(b[offset+0]) {
			n |= (1 << 3);
		}
		if(b[offset+1]) {
			n |= ( 1 << 2 );
		}
		if(b[offset+2]) {
			n |= ( 1 << 1);
		}
		if(b[offset+3]) {
			n |= 1;
		}
		sb.append(HexChar[n]);
	}

	
	
	public static final boolean[] parseByteMap(String text) {
		boolean[] b = ByteMap.newMap(false);
		CharReader r = new CharReader(text);
		char ch = r.readChar();
		while(ch != 0) {
			char next = r.readChar();
			if(next == '-') {
				int ch2 = r.readChar();
				if(ch > 0 && ch2 < 128) {
					ByteMap.appendRange(b, ch, ch2);
				}
				ch = r.readChar();
			}
			else {
				if(ch > 0 && ch < 128) {
					ByteMap.appendRange(b, ch, ch);
				}
				ch = next; //r.readChar();
			}
		}
		return b;
	}

	public final static String formatChar(int c) {
		if(Character.isISOControl(c) || c > 127) {
			return "<" + c + ">";
		}
		return "<" + (char)c + "," + c + ">";
	}

}
