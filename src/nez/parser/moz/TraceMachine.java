package nez.parser.moz;

import nez.Verbose;
import nez.io.SourceContext;
import nez.parser.TerminationException;
import nez.util.ConsoleUtils;
import nez.util.UList;

public class TraceMachine extends ParsingMachine {

	@Override
	public boolean run(MozInst code, SourceContext sc) {
		boolean result = false;
		String u = "Start";
		UList<String> stack = new UList<String>(new String[128]);
		stack.add("Start");
		try {
			while (true) {
				if (code instanceof Moz.Call) {
					stack.add(u);
					u = ((Moz.Call) code).getNonTerminalName();
				}
				if (code instanceof Moz.Ret) {
					u = stack.ArrayValues[stack.size() - 1];
					stack.clear(stack.size() - 1);
				}
				ConsoleUtils.println(u + "(" + sc.getPosition() + ")  " + code.id + " " + code);
				MozInst code2 = code.exec(sc);
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
