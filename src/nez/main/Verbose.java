package nez.main;

import java.lang.reflect.InvocationTargetException;

import nez.lang.Expression;
import nez.util.ConsoleUtils;

public class Verbose {
	public static final String BugsReport1 = "kimio@ynu.ac.jp";
	public static boolean General = true;

	public static boolean Example = false;

	public static boolean BacktrackActivity = false;
	public static boolean PackratParsing = false;

	public static boolean VirtualMachine = false;

	public static boolean ParsingExpression = false;

	public static boolean Grammar = false;

	public static boolean Debug = false;
	public static boolean SelfTesting = true;
	public static boolean NFA = true;
	public static boolean TraceException = true;
	public static boolean Time = false;

	public static void setAll() {
		General = true;
		Example = true;
		Grammar = true;
		ParsingExpression = true;
		VirtualMachine = true;
		PackratParsing = true;
		BacktrackActivity = true;
		Time = true;
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
		if(Time) {
			double d = (t2 - t1) / 1000000;
			ConsoleUtils.println(msg + ": " + String.format("%f", d) + "[ms]");
		}
	}

	public static void noticeOptimize(String key, Expression p) {
		if(ParsingExpression) {
			ConsoleUtils.println("optimizing " + key + "\n\t" + p);
		}
	}

	public static void noticeOptimize(String key, Expression p, Expression pp) {
		if(ParsingExpression) {
			ConsoleUtils.println("optimizing " + key + "\n\t" + p + "\n\t => " + pp);
		}
	}

	public final static void debug(Object s) {
		if(Command.ReleasePreview) {
			ConsoleUtils.println("debug: " + s);
		}
	}

	public final static void FIXME(Object s) {
		if(Command.ReleasePreview) {
			ConsoleUtils.println("FIXME: " + s);
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

	public static void traceException(Exception e) {
		if(TraceException) {
			if(e instanceof InvocationTargetException) {
				Throwable e2 = ((InvocationTargetException)e).getTargetException();
				if(e2 instanceof RuntimeException) {
					throw (RuntimeException)e2;
				}
			}
			e.printStackTrace();
		}
	}

	public static void printNoSuchMethodException(NoSuchMethodException e) {
		if(General) {
			ConsoleUtils.println(e);
		}
	}

}
