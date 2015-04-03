package nez.util;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class SourceBuilder {

	private OutputStream out;
	private String CHARSET = "UTF8";
	private final static String TAB = " ";
	private final static String LF = "\n";
	private final static String CRLF = "\r\n";
		
	public SourceBuilder(String fileName) throws IOException {
		this.out = new BufferedOutputStream(new FileOutputStream(fileName));
	}

	public void write(String text) {
		if(out == null) {
			System.out.print(text);
		}
		else {
			try {
				out.write(text.getBytes(CHARSET));
			} catch (IOException e) {
				ConsoleUtils.exit(1, "IO error: " + e.getMessage());
			}
		}
	}

//	public void write(String lf, String indent, String text) {
//		if(out == null) {
//			System.out.print(lf);
//			System.out.print(indent);
//			System.out.print(text);
//		}
//		else {
//			try {
//				out.write(lf.getBytes(CHARSET));
//				out.write(indent.getBytes(CHARSET));
//				out.write(text.getBytes(CHARSET));
//			} catch (IOException e) {
//				ConsoleUtils.exit(1, "IO error: " + e.getMessage());
//			}
//		}
//	}

	public void close() {
		if(out == null) {
			System.out.flush();
		}
		else {
			try {
				out.flush();
				out.close();
			} catch (IOException e) {
				ConsoleUtils.exit(1, "IO error: " + e.getMessage());
			}
			out = null;
		}
	}

}
