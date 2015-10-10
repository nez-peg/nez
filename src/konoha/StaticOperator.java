package konoha;

public class StaticOperator {

	/* boolean */
	public final static boolean opNot(boolean a) {
		return !a;
	}

	public final static boolean opEquals(boolean a, boolean b) {
		return a == b;
	}

	public final static boolean opNotEquals(boolean a, boolean b) {
		return a != b;
	}

	/* int */

	/* int */

	public final static int opPlus(int a) {
		return +a;
	}

	public final static int opMinus(int a) {
		return -a;
	}

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

	public final static int opLeftShift(int a, int b) {
		return a << b;
	}

	public final static int opRightShift(int a, int b) {
		return a >> b;
	}

	public final static int opLogicalRightShift(int a, int b) {
		return a >>> b;
	}

	public final static int opBitwiseAnd(int a, int b) {
		return a & b;
	}

	public final static int opBitwiseOr(int a, int b) {
		return a | b;
	}

	public final static int opBitwiseXor(int a, int b) {
		return a ^ b;
	}

	public final static int opCompl(int a) {
		return ~a;
	}

	/* double */
	public final static double opPlus(double a) {
		return +a;
	}

	public final static double opMinus(double a) {
		return -a;
	}

	public final static double opAdd(double a, double b) {
		return a + b;
	}

	public final static double opSub(double a, double b) {
		return a - b;
	}

	public final static double opMul(double a, double b) {
		return a * b;
	}

	public final static double opDiv(double a, double b) {
		return a / b;
	}

	public final static double opMod(double a, double b) {
		return a % b;
	}

	public final static boolean opEquals(double a, double b) {
		return a == b;
	}

	public final static boolean opNotEquals(double a, double b) {
		return a != b;
	}

	public final static boolean opLessThan(double a, double b) {
		return a < b;
	}

	public final static boolean opGreaterThan(double a, double b) {
		return a > b;
	}

	public final static boolean opLessThanEquals(double a, double b) {
		return a <= b;
	}

	public final static boolean opGreaterThanEquals(double a, double b) {
		return a >= b;
	}

	/* long */

	public final static long opPlus(long a) {
		return +a;
	}

	public final static long opMinus(long a) {
		return -a;
	}

	public final static long opAdd(long a, long b) {
		return a + b;
	}

	public final static long opSub(long a, long b) {
		return a - b;
	}

	public final static long opMul(long a, long b) {
		return a * b;
	}

	public final static long opDiv(long a, long b) {
		return a / b;
	}

	public final static long opMod(long a, long b) {
		return a % b;
	}

	public final static boolean opEquals(long a, long b) {
		return a == b;
	}

	public final static boolean opNotEquals(long a, long b) {
		return a != b;
	}

	public final static boolean opLessThan(long a, long b) {
		return a < b;
	}

	public final static boolean opGreaterThan(long a, long b) {
		return a > b;
	}

	public final static boolean opLessThanEquals(long a, long b) {
		return a <= b;
	}

	public final static boolean opGreaterThanEquals(long a, long b) {
		return a >= b;
	}

	public final static long opLeftShift(long a, long b) {
		return a << b;
	}

	public final static long opRightShift(long a, long b) {
		return a >> b;
	}

	public final static long opLogicalRightShift(long a, long b) {
		return a >>> b;
	}

	public final static long opBitwiseAnd(long a, long b) {
		return a & b;
	}

	public final static long opBitwiseOr(long a, long b) {
		return a | b;
	}

	public final static long opBitwiseXor(long a, long b) {
		return a ^ b;
	}

	public final static long opCompl(long a) {
		return ~a;
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

	public final static long to_long(short a) {
		return a;
	}

	public final static long to_long(Short a) {
		return a.longValue();
	}

	public final static long to_long(byte a) {
		return a & 0xff;
	}

	public final static long to_long(Byte a) {
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

	public final static int to_int(short a) {
		return a;
	}

	public final static int to_int(Short a) {
		return a.intValue();
	}

	public final static int to_int(byte a) {
		return a & 0xff;
	}

	public final static int to_int(Byte a) {
		return a.intValue() & 0xff;
	}

}
