package nez.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;

import jline.ConsoleReader;

public class ConsoleUtils {
	static boolean isColored = false;
	static {
		if (System.getenv("CLICOLOR") != null) {
			isColored = true;
		}
	}

	public final static void exit(int status, String message) {
		ConsoleUtils.println("EXIT " + message);
		System.exit(status);
	}

	// 31 :red 　　　32 green, 34 blue, 37 gray
	public final static void begin(int c) {
		if (isColored) {
			System.out.print("\u001b[00;" + c + "m");
		}
	}

	public final static void bold() {
		if (isColored) {
			System.out.print("\u001b[1m");
		}
	}

	public final static void end() {
		if (isColored) {
			System.out.print("\u001b[00m");
		}
	}

	public final static void println(Object s) {
		System.out.println(s);
	}

	public final static void print(Object s) {
		System.out.print(s);
	}

	public final static void print(String tab, Object o) {
		System.out.print(tab);
		String s = o.toString();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '\n') {
				System.out.println();
				System.out.print(tab);
			} else {
				System.out.print(c);
			}
		}
	}

	public final static void println(String tab, Object o) {
		print(tab, o);
		System.out.println();
	}

	public final static boolean isDebug() {
		return System.getenv("DEBUG") != null;
	}

	public final static void debug(String s) {
		System.out.println(s);
	}

	public static void notice(String message) {
		System.out.println("NOTICE: " + message);
	}

	// console

	private static Object console = null;

	public final static Object getConsoleReader() {
		if (console == null) {
			try {
				console = Class.forName("jline.ConsoleReader").newInstance();
			} catch (Exception e) {
				System.err.println("CHECK: " + e);
			}
		}
		return console;
	}

	@SuppressWarnings("resource")
	public final static String readSingleLine(Object console, String prompt) {
		if (!(console instanceof Scanner)) {
			try {
				Method m = console.getClass().getMethod("readLine", String.class);
				return (String) m.invoke(console, prompt);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		System.out.print(prompt);
		System.out.flush();
		return new Scanner(System.in).nextLine();
	}

	public final static void addHistory(Object console, String text) {
		if (console != null) {
			try {
				Method m = console.getClass().getMethod("getHistory");
				Object hist = m.invoke(console);
				m = hist.getClass().getMethod("addToHistory", String.class);
				m.invoke(hist, text);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public final static void addCompleter(List<String> list) {
		Object con = getConsoleReader();
		if (con != null) {
			try {
				Class<?> c = Class.forName("jline.console.completer.StringsCompleter");
				Constructor<?> nc = c.getConstructor(Collection.class);
				Method m = con.getClass().getMethod("addCompletor");
				m.invoke(con, nc.newInstance(list));
				return;
			} catch (Exception e) {
			}
			try {
				// Class<?> c = Class.forName("jline.SimpleCompletor");
				// Constructor<?> nc = c.getConstructor(String[].class);
				// Method m = console.getClass().getMethod("addCompletor",
				// jline.Completor.class);
				String[] s = list.toArray(new String[list.size()]);
				((ConsoleReader) con).addCompletor(new jline.SimpleCompletor(s));
				// m.invoke(console, nc.newInstance((Object[])s));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public final static String readMultiLine(String prompt, String prompt2) {
		Object console = getConsoleReader();
		StringBuilder sb = new StringBuilder();
		String line;
		while (true) {
			line = readSingleLine(console, prompt);
			if (line == null) {
				return null;
			}
			if (line.equals("")) {
				break;
			}
			sb.append(line);
			sb.append("\n");
			prompt = prompt2;
		}
		line = sb.toString();
		addHistory(console, line);
		return line;
	}

}
