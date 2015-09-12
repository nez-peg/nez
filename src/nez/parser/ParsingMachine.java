package nez.parser;

import nez.SourceContext;

public class ParsingMachine {

	public boolean run(Instruction code, SourceContext sc) {
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

}
