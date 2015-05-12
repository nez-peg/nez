package nez.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import jline.ConsoleReader;


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
	
	// console
	
	private static Object console = null;
	public final static Object getConsoleReader() {
		if(console == null) {
			try {
				console = Class.forName("jline.ConsoleReader").newInstance();
			}
			catch(Exception e) {
				System.err.println("CHECK: " + e);
			}
		}
		return console;
	}
	
	@SuppressWarnings("resource")
	public final static String readSingleLine(Object console, String prompt) {
		if(!(console instanceof Scanner)) {
			try {
				Method m = console.getClass().getMethod("readLine", String.class);
				return (String)m.invoke(console, prompt);
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
		System.out.print(prompt);
		System.out.flush();
		return new Scanner(System.in).nextLine();
	}

	public final static void addHistory(Object console, String text) {
		if(console != null) {
			try {
				Method m = console.getClass().getMethod("getHistory");
				Object hist = m.invoke(console);
				m = hist.getClass().getMethod("addToHistory", String.class);
				m.invoke(hist, text);
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public final static void addCompleter(List<String> list) {
		Object console = getConsoleReader();
		if(console != null) {
			try {
				Class<?> c = Class.forName("jline.console.completer.StringsCompleter");
				Constructor<?> nc = c.getConstructor(Collection.class);
				Method m = console.getClass().getMethod("addCompletor");
				 m.invoke(console, nc.newInstance(list));
				 return;
			}
			catch(Exception e) {
			}
			try {
//				Class<?> c = Class.forName("jline.SimpleCompletor");
//				Constructor<?> nc = c.getConstructor(String[].class);
//				Method m = console.getClass().getMethod("addCompletor", jline.Completor.class);
				String[] s = list.toArray(new String[list.size()]);
				((ConsoleReader)console).addCompletor(new jline.SimpleCompletor(s));
				//m.invoke(console, nc.newInstance((Object[])s));
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
	}

	public final static String readMultiLine(String prompt, String prompt2) {
		Object console = getConsoleReader();
		StringBuilder sb = new StringBuilder();
		String line;
		while(true) {
			line = readSingleLine(console, prompt);
			if(line == null) {
				return null;
			}
			if(line.equals("")) {
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
