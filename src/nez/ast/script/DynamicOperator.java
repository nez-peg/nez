package nez.ast.script;

public class DynamicOperator {
	public static final Object opAdd(Object x, Object y) {
		if (x instanceof String || y instanceof String) {
			return "" + x + y;
		}
		if (x instanceof Number && y instanceof Number) {
			return ((Number) x).intValue() + ((Number) y).intValue();
		}
		return null;
	}

	public static final Integer opSub(Integer x, Integer y) {
		return x - y;
	}

	public static final Integer opMul(Integer x, Integer y) {
		return x * y;
	}

	public static final Integer opDiv(Integer x, Integer y) {
		return x / y;
	}
}
