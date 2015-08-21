package nez.ast.jcode;

public class JCodeOperator {
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

	public static int Div(int x, int y) {
		return x / y;
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
