package nez.ext;

import java.io.IOException;

import nez.Parser;
import nez.Verbose;
import nez.io.SourceContext;
import nez.main.Command;
import nez.main.CommandContext;
import nez.util.ConsoleUtils;
import nez.util.StringUtils;
import nez.util.UList;

public class Cmatch extends Command {
	@Override
	public void exec(CommandContext config) throws IOException {
		config.getStrategy().setEnabled("ast", false);
		Parser g = config.newParser();

		UList<String> failedInputs = new UList<String>(new String[4]);
		UList<String> unconsumedInputs = new UList<String>(new String[4]);

		int totalCount = 0, failureCount = 0, unconsumedCount = 0;
		long consumed = 0;
		long time = 0;

		while (config.hasInput()) {
			SourceContext file = config.nextInput();
			totalCount++;
			long t = System.nanoTime();
			boolean result = g.match(file);
			long t2 = System.nanoTime();
			if (!result) {
				ConsoleUtils.println(file.getSyntaxErrorMessage());
				failedInputs.add(file.getResourceName());
				failureCount++;
				continue;
			}
			if (file.hasUnconsumed()) {
				ConsoleUtils.println(file.getUnconsumedMessage());
				unconsumedInputs.add(file.getResourceName());
				unconsumedCount++;
			}
			consumed += file.getPosition();
			time += (t2 - t);
			g.logProfiler();
		}
		if (totalCount > 1) {
			Verbose.println(totalCount + " files, " + StringUtils.formatMPS(consumed, time) + " MiB/s, " + failureCount + " failed, " + unconsumedCount + " uncosumed, "
					+ StringUtils.formatParcentage(totalCount - (unconsumedCount + failureCount), totalCount) + "% passed.");
		}
		if (unconsumedInputs.size() > 0) {
			Verbose.println("unconsumed: " + unconsumedInputs);
		}
		if (failedInputs.size() > 0) {
			ConsoleUtils.exit(1, "failed: " + failedInputs);
		}
	}
}