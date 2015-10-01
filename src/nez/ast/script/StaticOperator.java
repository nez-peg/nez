package nez.ast.script;

public class StaticOperator {
	/* double */
	public final static double opAdd(double a, double b) {
		return a + b;
	}

	/* long */

	/* int */

	public final static int opAdd(int a, int b) {
		return a + b;
	}

	public final static int opSub(int a, int b) {
		return a - b;
	}

	public final static int opMul(int a, int b) {
		return a * b;
	}

	public final static int opDiv(int a, int b) {
		return a / b;
	}

	public final static int opMod(int a, int b) {
		return a % b;
	}

	public final static boolean opEquals(int a, int b) {
		return a == b;
	}

	public final static boolean opNotEquals(int a, int b) {
		return a != b;
	}

	public final static boolean opLessThan(int a, int b) {
		return a < b;
	}

	public final static boolean opGreaterThan(int a, int b) {
		return a > b;
	}

	public final static boolean opLessThanEquals(int a, int b) {
		return a <= b;
	}

	public final static boolean opGreaterThanEquals(int a, int b) {
		return a >= b;
	}

	/* double */

	public final static double to_double(Double a) {
		return a.doubleValue();
	}

	public final static double to_double(float a) {
		return a;
	}

	public final static double to_double(Float a) {
		return a.doubleValue();
	}

	public final static double to_double(long a) {
		return a;
	}

	public final static double to_double(Long a) {
		return a.doubleValue();
	}

	public final static double to_double(int a) {
		return a;
	}

	public final static double to_double(Integer a) {
		return a.doubleValue();
	}

	/* long */

	public final static long to_long(Long a) {
		return a.longValue();
	}

	public final static long to_long(double a) {
		return (long) a;
	}

	public final static long to_long(Double a) {
		return a.longValue();
	}

	public final static long to_long(float a) {
		return (long) a;
	}

	public final static long to_long(Float a) {
		return a.longValue();
	}

	public final static long to_long(int a) {
		return a;
	}

	public final static long to_long(Integer a) {
		return a.longValue();
	}

	/* int */

	public final static int to_int(Integer a) {
		return a.intValue();
	}

	public final static int to_int(double a) {
		return (int) a;
	}

	public final static int to_int(Double a) {
		return a.intValue();
	}

	public final static int to_int(float a) {
		return (int) a;
	}

	public final static int to_int(Float a) {
		return a.intValue();
	}

	public final static int to_int(long a) {
		return (int) a;
	}

	public final static int to_int(Long a) {
		return a.intValue();
	}

}
