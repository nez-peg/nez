package nez.main;

import nez.NezOption;
import nez.SourceContext;
import nez.lang.Grammar;
import nez.util.ConsoleUtils;
import nez.util.UList;

class LCvalidate extends Command {
	@Override
	public String getDesc() {
		return "grammar validator";
	}

	@Override
	public void exec(CommandContext config) {
		UList<String> failedInput = new UList<String>(new String[4]);
		config.getNezOption().setOption("ast", false);
		Grammar g = config.getGrammar();
		int totalCount = 0, failureCount = 0, unconsumedCount = 0;
		while(config.hasInputSource()) {
			SourceContext file = config.nextInputSource();
			totalCount++;
			boolean result = g.match(file);
			g.verboseMemo();
			if(!result) {
				ConsoleUtils.println(file.getSyntaxErrorMessage());
				failedInput.add(file.getResourceName());
				failureCount++;
				continue;
			}
			if(file.hasUnconsumed()) {
				ConsoleUtils.println(file.getUnconsumedMessage());
				unconsumedCount++;
			}
			g.logProfiler();
		}
		if(totalCount > 1){
			Verbose.println(
					totalCount + " files, " +
					failureCount + " failed, " +
					unconsumedCount + " uncosumed, " +
					(100 - 100.0 * (unconsumedCount+failureCount)/totalCount) + "% passed.");
		}
		if(failedInput.size() > 0) {
			ConsoleUtils.exit(1, "failed: " + failedInput);
		}
	}

}