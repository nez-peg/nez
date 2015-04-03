package nez.util;

import java.io.IOException;


public class ConsoleUtils {

	public final static void exit(int status, String message) {
		ConsoleUtils.println("EXIT " + message);
		System.exit(status);
	}
	
	public final static void println(Object s) {
		System.out.println(s);
	}

	public final static void print(Object s) {
		System.out.print(s);
	}

	public final static void debug(String s) {
		System.out.println(s);
	}

	public static void notice(String message) {
		System.out.println("NOTICE: " + message);
	}
	
	private static jline.ConsoleReader ConsoleReader = null;

	public final static String readMultiLine(String prompt, String prompt2) {
		if(ConsoleReader == null) {
			try {
				ConsoleReader = new jline.ConsoleReader();
			}
			catch(IOException e) {
				throw new RuntimeException(e);
			}
		}
		StringBuilder sb = new StringBuilder();
		String line;
		while(true) {
			line = readSingleLine(prompt);
			if(line == null) {
				return null;
			}
			if(line.equals("")) {
				break;
			}
			sb.append(line.substring(0, line.length() - 1));
			sb.append("\n");
			prompt = prompt2;
		}
		line = sb.toString();
		ConsoleReader.getHistory().addToHistory(line);
		return line;
	}

	private final static String readSingleLine(String prompt) {
		try {
			return ConsoleReader.readLine(prompt);
		}
		catch(IOException e) {
			e.printStackTrace();
			return null;
		}
	}

}
