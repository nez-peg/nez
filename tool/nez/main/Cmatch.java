package nez.main;

import java.io.IOException;

import nez.ast.Source;
import nez.parser.Parser;
import nez.util.ConsoleUtils;
import nez.util.StringUtils;
import nez.util.UList;
import nez.util.Verbose;

public class Cmatch extends Command {
	@Override
	public void exec() throws IOException {
		strategy.TreeConstruction = false;
		Parser parser = newParser();

		UList<String> failedInputs = new UList<String>(new String[4]);

		int totalCount = 0, failureCount = 0, unconsumedCount = 0;
		long consumed = 0;
		long time = 0;

		while (hasInputSource()) {
			Source file = nextInputSource();
			totalCount++;

			long t = System.nanoTime();
			boolean result = parser.match(file);
			long t2 = System.nanoTime();
			if (parser.hasErrors()) {
				parser.showErrors();
				failedInputs.add(file.getResourceName());
				failureCount++;
			}
			consumed += file.length();
			time += (t2 - t);
			parser.logProfiler();
		}
		if (totalCount > 1) {
			Verbose.println(totalCount + " files, " + StringUtils.formatMPS(consumed, time) + " MiB/s, " + failureCount + " failed, " + unconsumedCount + " uncosumed, "
					+ StringUtils.formatParcentage(totalCount - (unconsumedCount + failureCount), totalCount) + "% passed.");
		}
		if (failedInputs.size() > 0) {
			ConsoleUtils.exit(1, "failed: " + failedInputs);
		}
	}
}