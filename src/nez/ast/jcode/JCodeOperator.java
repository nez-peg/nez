package nez.ast.jcode;

public class JCodeOperator {

	public static Object Plus(Object x) {
		if(x instanceof Number) {
			return x;
		}
		throw new RuntimeException("unsupported operator + " + x);
	}

	public static Object Minus(Object x) {
		if(x instanceof Double) {
			return -((Long) x).doubleValue();
		}
		return -((Number) x).longValue();
	}

	public static long BitwiseNot(Object x) {
		if(x instanceof Number && !(x instanceof Double)) {
			return ~((Number) x).longValue();
		}
		throw new RuntimeException("unsupported operator ~ " + x);
	}

	public static Object Add(Object x, Object y) {
		if(x instanceof String || y instanceof String) {
			return "" + x + y;
		}
		if(x instanceof Double || y instanceof Double) {
			return ((Number) x).doubleValue() + ((Number) y).doubleValue();
		}
		return ((Number) x).longValue() + ((Number) y).longValue();
	}

	public static Object Sub(Object x, Object y) {
		if(x instanceof Double || y instanceof Double) {
			return ((Double) x).doubleValue() - ((Double) y).doubleValue();
		}
		return ((Number) x).longValue() - ((Number) y).longValue();
	}

	public static Object Mul(Object x, Object y) {
		if(x instanceof Double || y instanceof Double) {
			return ((Number) x).doubleValue() * ((Number) y).doubleValue();
		}
		return ((Number) x).longValue() * ((Number) y).longValue();
	}

	public static Object Div(Object x, Object y) {
		if(x instanceof Double || y instanceof Double) {
			return ((Number) x).doubleValue() / ((Number) y).doubleValue();
		}
		return ((Number) x).longValue() / ((Number) y).longValue();
	}

	public static Object Mod(Object x, Object y) {
		if(x instanceof Double || y instanceof Double) {
			return ((Number) x).doubleValue() % ((Number) y).doubleValue();
		}
		return ((Number) x).longValue() / ((Number) y).longValue();
	}

	public static long LeftShift(Object x, Object y) {
		return ((Number) x).longValue() << ((Number) y).longValue();
	}

	public static long RightShift(Object x, Object y) {
		return ((Number) x).longValue() >> ((Number) y).longValue();
	}

	public static long BitwiseAnd(Object x, Object y) {
		return ((Number) x).longValue() & ((Number) y).longValue();
	}

	public static long BitwiseOr(Object x, Object y) {
		return ((Number) x).longValue() | ((Number) y).longValue();
	}

	public static long BitwiseXor(Object x, Object y) {
		return ((Number) x).longValue() ^ ((Number) y).longValue();
	}

	public static boolean LogicalAnd(boolean x, boolean y){
		return x && y;
	}

	public static boolean LogicalOr(boolean x, boolean y){
		return x || y;
	}

	public static boolean LessThan(Object x, Object y) {
		return ((Number) x).doubleValue() < ((Number) y).doubleValue();
	}

	public static boolean LessThanEquals(Object x, Object y) {
		return ((Number) x).doubleValue() <= ((Number) y).doubleValue();
	}

	public static boolean GreaterThan(Object x, Object y) {
		return ((Number) x).doubleValue() > ((Number) y).doubleValue();
	}

	public static boolean GreaterThanEquals(Object x, Object y) {
		return ((Number) x).doubleValue() >= ((Number) y).doubleValue();
	}

	public static boolean Equals(Object x, Object y) {
		if(x instanceof Number && y instanceof Number) {
			return ((Number) x).doubleValue() == ((Number) y).doubleValue();
		}
		return x == y;
	}

	public static boolean NotEquals(Object x, Object y) {
		if(x instanceof Number && y instanceof Number) {
			return ((Number) x).doubleValue() != ((Number) y).doubleValue();
		}
		return x != y;
	}

	public static int Minus(int x) {
		return -x;
	}

	public static double Minus(double x) {
		return -x;
	}

	public static boolean LogicalNot(boolean x) {
		return !x;
	}

	public static int Add(int x, int y) {
		return x + y;
	}

	public static int Sub(int x, int y) {
		return x - y;
	}

	public static int Mul(int x, int y) {
		return x * y;
	}

	public static double Div(int x, int y) {
		return (double) x / (double) y;
	}

	public static double Add(int x, double y) {
		return (double) x + y;
	}

	public static double Add(double x, int y) {
		return x + (double) y;
	}

	public static double Sub(int x, double y) {
		return (double) x - y;
	}

	public static double Sub(double x, int y) {
		return x - (double) y;
	}

	public static double Mul(int x, double y) {
		return (double) x * y;
	}

	public static double Mul(double x, int y) {
		return x * (double) y;
	}

	public static double Div(int x, double y) {
		return (double) x / y;
	}

	public static double Div(double x, int y) {
		return x / (double) y;
	}

	public static double Add(double x, double y) {
		return x + y;
	}

	public static double Sub(double x, double y) {
		return x - y;
	}

	public static double Mul(double x, double y) {
		return x * y;
	}

	public static double Div(double x, double y) {
		return x / y;
	}

	public static boolean LessThan(double x, double y) {
		return x < y;
	}

	public static boolean LessThanEquals(double x, double y) {
		return x <= y;
	}

	public static boolean GreaterThan(double x, double y) {
		return x > y;
	}

	public static boolean GreaterThanEquals(double x, double y) {
		return x >= y;
	}

	public static boolean Equals(double x, double y) {
		return x == y;
	}

	public static boolean NotEquals(double x, double y) {
		return x != y;
	}

	public static boolean LessThan(int x, int y) {
		return x < y;
	}

	public static boolean LessThanEquals(int x, int y) {
		return x <= y;
	}

	public static boolean GreaterThan(int x, int y) {
		return x > y;
	}

	public static boolean GreaterThanEquals(int x, int y) {
		return x >= y;
	}

	public static boolean Equals(int x, int y) {
		return x == y;
	}

	public static boolean NotEquals(int x, int y) {
		return x != y;
	}

	public static boolean LessThan(boolean x, boolean y) {
		return false;
	}

	public static boolean LessThanEquals(boolean x, boolean y) {
		return false;
	}

	public static boolean GreaterThan(boolean x, boolean y) {
		return false;
	}

	public static boolean GreaterThanEquals(boolean x, boolean y) {
		return false;
	}

	public static boolean Equals(boolean x, boolean y) {
		return x == y;
	}

	public static boolean NotEquals(boolean x, boolean y) {
		return x != y;
	}
}
