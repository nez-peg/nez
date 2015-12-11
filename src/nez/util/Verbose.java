package nez.util;

import java.lang.reflect.InvocationTargetException;

import nez.Version;
import nez.lang.Expression;

public class Verbose {
	public static boolean enabled = false;
	/* obsolete */
	public static boolean BacktrackActivity = false;
	public static boolean PackratParsing = false;

	public final static void println(String s) {
		if (enabled) {
			ConsoleUtils.begin(34);
			ConsoleUtils.println(s);
			ConsoleUtils.end();
		}
	}

	public final static void println(String fmt, Object... args) {
		if (enabled) {
			println(String.format(fmt, args));
		}
	}

	public final static void print(String s) {
		if (enabled) {
			ConsoleUtils.begin(34);
			ConsoleUtils.print(s);
			ConsoleUtils.end();
		}
	}

	public final static void print(String fmt, Object... args) {
		if (enabled) {
			print(String.format(fmt, args));
		}
	}

	public static void traceException(Exception e) {
		if (enabled) {
			if (e instanceof InvocationTargetException) {
				Throwable e2 = ((InvocationTargetException) e).getTargetException();
				if (e2 instanceof RuntimeException) {
					throw (RuntimeException) e2;
				}
			}
			ConsoleUtils.begin(34);
			e.printStackTrace();
			ConsoleUtils.end();
		}
	}

	public static void TODO(String s) {
		println("[TODO] " + s);
	}

	public static void TODO(String fmt, Object... args) {
		println("[TODO] " + String.format(fmt, args));
	}

	public final static void printElapsedTime(String msg, long t1, long t2) {
		if (enabled) {
			double d = (t2 - t1) / 1000000;
			println("%s : %f[ms]", msg, d);
		}
	}

	public static void noticeOptimize(String key, Expression p) {
		// if (enabled) {
		// ConsoleUtils.println("optimizing " + key + "\n\t" + p);
		// }
	}

	public static void noticeOptimize(String key, Expression p, Expression pp) {
		// if (enabled) {
		// ConsoleUtils.println("optimizing " + key + "\n\t" + p + "\n\t => " +
		// pp);
		// }
	}

	public final static void debug(Object s) {
		if (Version.ReleasePreview) {
			ConsoleUtils.println("debug: " + s);
		}
	}

	public final static void FIXME(Object s) {
		if (Version.ReleasePreview) {
			ConsoleUtils.println("FIXME: " + s);
		}
	}

}
