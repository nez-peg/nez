package nez.vm;

import nez.SourceContext;
import nez.util.ConsoleUtils;
import nez.util.UList;

public class Machine {

	public static boolean run(Instruction code, SourceContext sc) {
		boolean result = false;
		try {
			while (true) {
				code = code.exec(sc);
			}
		} catch (TerminationException e) {
			result = e.status;
		}
		return result;
	}

	public static boolean debug(Instruction code, SourceContext sc) {
		boolean result = false;
		String u = "Start";
		UList<String> stack = new UList<String>(new String[128]);
		stack.add("Start");
		try {
			while (true) {
				if(code instanceof ICallPush) {
					stack.add(u);
					u = ((ICallPush) code).rule.getLocalName();
				}
				if(code instanceof IRet) {
					u = stack.ArrayValues[stack.size() - 1];
					stack.clear(stack.size() - 1);
				}
				ConsoleUtils.println(u + "(" + sc.getPosition() + ")  " + code.id + " " + code);
				code = code.exec(sc);
			}
		} catch (TerminationException e) {
			result = e.status;
		}
		return result;
	}

}
