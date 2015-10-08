package konoha;

public class libc {

	public final static void printf(String fmt) {
		System.out.print(String.format(fmt));
	}

	public final static void printf(String fmt, Object s) {
		System.out.print(String.format(fmt, s));
	}

	public final static void printf(String fmt, Object s, Object s2) {
		System.out.print(String.format(fmt, s, s2));
	}

	public final static void printf(String fmt, Object s, Object s2, Object s3) {
		System.out.print(String.format(fmt, s, s2, s3));
	}

	public final static void printf(String fmt, Object s, Object s2, Object s3, Object s4) {
		System.out.print(String.format(fmt, s, s2, s3, s4));
	}

	public final static void print(Object s) {
		System.out.print(s);
	}

	public final static void println() {
		System.out.println();
	}

	public final static void println(Object s) {
		System.out.println(s);
	}

	public final static void println(String fmt, Object s) {
		System.out.println(String.format(fmt, s));
	}

	public final static void println(String fmt, Object s, Object s2) {
		System.out.println(String.format(fmt, s, s2));
	}

	public final static void println(String fmt, Object s, Object s2, Object s3) {
		System.out.println(String.format(fmt, s, s2, s3));
	}

	public final static void println(String fmt, Object s, Object s2, Object s3, Object s4) {
		System.out.println(String.format(fmt, s, s2, s3, s4));
	}

}
