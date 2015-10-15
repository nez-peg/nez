package konoha.script;

import java.util.HashMap;

public class OperatorNames {
	static HashMap<String, String> names = new HashMap<>();

	static void s(String n, String m) {
		names.put(n, m);
		names.put(m, n);
	}

	static {
		s("+", "opAdd");
		s("-", "opSub");
		s("*", "opMul");
		s("/", "opDiv");
		s("%", "opMod");
		s("==", "opEquals");
		s("!=", "opNotEquals");
		s("<=", "opLessThanEquals");
		s("<", "opLessThan");
		s(">", "opGreaterThan");
		s(">=", "opGreaterThanEquals");
		s("&", "opBitwiseAnd");
		s("|", "opBitwiseOr");
		s("^", "opBitwiseXor");
		s("~", "opCompl");
	}

	public final static String name(String n) {
		return names.get(n);
	}
}
