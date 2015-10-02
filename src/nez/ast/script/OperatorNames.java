package nez.ast.script;

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
	}

	public final static String name(String n) {
		return names.get(n);
	}
}
