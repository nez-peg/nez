package nez.main;

import nez.expr.Expression;
import nez.util.ConsoleUtils;

public class Verbose {
	public static final String BugsReport1 = "kimio@ynu.ac.jp";
	public static boolean General = true;
	public static boolean Grammar = false;
	public static boolean Expression = false;
	public static boolean VirtualMachine = false;
	public static boolean PackratParsing = false;
	public static boolean Debug = false;
	public static boolean Backtrack = false;
	public static boolean SelfTesting = true;
	public static boolean NFA = true;
	
	
	public static void setAll() {
		General = true;
		Grammar = true;
		Expression = true;
		VirtualMachine = true;
		PackratParsing = true;
		Backtrack = true;
	}

	public final static void print(String msg) {
		if(General) {
			ConsoleUtils.println(msg);
		}
	}

	public final static void println(String msg) {
		if(General) {
			ConsoleUtils.println(msg);
		}
	}
	
	public static void todo(Object msg) {
		if(General) {
			ConsoleUtils.println("TODO " + msg);
		}
	}
	
	public final static void printElapsedTime(String msg, long t1, long t2) {
		double d = (t2 - t1) / 1000000;
		println(msg + ": " + String.format("%f", d) + "[ms]");
	}

	public static void noticeOptimize(String key, Expression p) {
		if(Expression) {
			ConsoleUtils.println("optimizing " + key + "\n\t" + p);
		}
	}

	public static void noticeOptimize(String key, Expression p, Expression pp) {
		if(Expression) {
			ConsoleUtils.println("optimizing " + key + "\n\t" + p + "\n\t => " + pp);
		}
	}

	public static void debug(Object s) {
		if(Debug) {
			ConsoleUtils.println("debug: " + s);
		}
	}

	public final static void printSelfTesting(Object s) {
		if(SelfTesting) {
			ConsoleUtils.println(s);
		}
	}

	public final static void printSelfTestingIndent(Object s) {
		if(SelfTesting) {
			ConsoleUtils.println("   " + s);
		}
	}

	public final static void printNFA(Object s) {
		if(NFA) {
			ConsoleUtils.println("NFA: " + s);
		}
	}

	
}
