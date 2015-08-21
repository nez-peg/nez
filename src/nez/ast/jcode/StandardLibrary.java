package nez.ast.jcode;

import java.util.HashMap;
import java.util.Map;

public class StandardLibrary {
	static Map<String, InvocationTarget> targetMap = new HashMap<String, InvocationTarget>();

	class console {
		public void log(int x) {
			System.out.println(x);
		}

		public void log(double x) {
			System.out.println(x);
		}

		public void log(boolean x) {
			System.out.println(x);
		}

		public void log(Object x) {
			System.out.println(x);
		}
	}

	static {
		Import("console.log", InvocationTarget.newVirtualTarget(console.class, void.class, "log", int.class));
		Import("console.log", InvocationTarget.newVirtualTarget(console.class, void.class, "log", double.class));
		Import("console.log", InvocationTarget.newVirtualTarget(console.class, void.class, "log", boolean.class));
		Import("console.log", InvocationTarget.newVirtualTarget(console.class, void.class, "log", Object.class));
	}

	public static void Import(String name, InvocationTarget target) {
		targetMap.put(name, target);
	}

	public static InvocationTarget get(String name) {
		return targetMap.get(name);
	}

}
