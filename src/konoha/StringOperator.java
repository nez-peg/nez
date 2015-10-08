package konoha;

public class StringOperator {

	public static final boolean opEquals(String x, String y) {
		return x.equals(y);
	}

	public static final boolean opNotEquals(String x, String y) {
		return !x.equals(y);
	}

	public final static boolean opLessThan(String a, String b) {
		return a.compareTo(b) < 0;
	}

	public final static boolean opGreaterThan(String a, String b) {
		return a.compareTo(b) > 0;
	}

	public final static boolean opLessThanEquals(String a, String b) {
		return a.compareTo(b) <= 0;
	}

	public final static boolean opGreaterThanEquals(String a, String b) {
		return a.compareTo(b) >= 0;
	}

	public final static String toString(int x) {
		return String.valueOf(x);
	}

	public final static int toint(String x) {
		try {
			return Integer.parseInt(x);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	public final static String toString(Object x) {
		return x == null ? "null" : x.toString();
	}

}
