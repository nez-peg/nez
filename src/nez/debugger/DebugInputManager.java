package nez.debugger;

import java.io.IOException;

import nez.Parser;
import nez.util.ConsoleUtils;
import nez.util.UList;

public class DebugInputManager {
	private int index;
	public UList<String> inputFileLists;

	public DebugInputManager(UList<String> inputFileLists) {
		this.inputFileLists = inputFileLists;
		this.index = 0;
	}

	public void exec(Parser peg) {
		while (this.index < this.inputFileLists.size()) {
			DebugSourceContext sc = this.nextInputSource();
			// peg.debug(sc); FIXME
		}
	}

	public final DebugSourceContext nextInputSource() {
		if (this.index < this.inputFileLists.size()) {
			String f = this.inputFileLists.ArrayValues[this.index];
			this.index++;
			try {
				return DebugSourceContext.newDebugFileContext(f);
			} catch (IOException e) {
				ConsoleUtils.exit(1, "cannot open: " + f);
			}
		}
		ConsoleUtils.exit(1, "error: input file list is empty");
		return null;
	}
}
