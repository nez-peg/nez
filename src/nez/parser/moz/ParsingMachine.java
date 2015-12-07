package nez.parser.moz;

import nez.io.SourceContext;
import nez.parser.TerminationException;

public class ParsingMachine {

	public boolean run(MozInst code, SourceContext sc) {
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
