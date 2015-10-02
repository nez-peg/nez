package nez.ast.script;

import java.util.Comparator;

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

	@SuppressWarnings("unchecked")
	public static final Object opEquals(Object x, Object y) {
		if (x instanceof Comparator<?>) {
			return ((Comparator<Object>) x).compare(x, y) == 0;
		}
		return x == y;
	}

	@SuppressWarnings("unchecked")
	public static final Object opNotEquals(Object x, Object y) {
		if (x instanceof Comparator<?>) {
			return ((Comparator<Object>) x).compare(x, y) != 0;
		}
		return x != y;
	}

}
