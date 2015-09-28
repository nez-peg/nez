package nez.ast.script;

public class DynamicOperator {
	Object opAdd(Object x, Object y) {
		if (x instanceof String || y instanceof String) {
			return "" + x + y;
		}
		return null;
	}
}
