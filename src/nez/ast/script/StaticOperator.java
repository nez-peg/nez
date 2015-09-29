package nez.ast.script;

public class StaticOperator {
	public final static int opAdd(int a, int b) {
		return a + b;
	}

	public final static int opSub(int a, int b) {
		return a - b;
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
}
