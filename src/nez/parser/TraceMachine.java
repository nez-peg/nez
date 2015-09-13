package nez.parser;

import nez.io.SourceContext;
import nez.main.Verbose;
import nez.util.ConsoleUtils;
import nez.util.UList;

public class TraceMachine extends ParsingMachine {

	@Override
	public boolean run(Instruction code, SourceContext sc) {
		boolean result = false;
		String u = "Start";
		UList<String> stack = new UList<String>(new String[128]);
		stack.add("Start");
		try {
			while (true) {
				if (code instanceof ICall) {
					stack.add(u);
					u = ((ICall) code).prod.getLocalName();
				}
				if (code instanceof IRet) {
					u = stack.ArrayValues[stack.size() - 1];
					stack.clear(stack.size() - 1);
				}
				ConsoleUtils.println(u + "(" + sc.getPosition() + ")  " + code.id + " " + code);
				Instruction code2 = code.exec(sc);
				if (code2 == null) {
					Verbose.debug("@@ returning null at " + code);
				}
				code = code2;
			}
		} catch (TerminationException e) {
			result = e.status;
		}
		return result;
	}

}
