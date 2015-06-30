package nez.main;

import nez.SourceContext;
import nez.ast.CommonTree;
import nez.ast.CommonTreeWriter;
import nez.lang.Grammar;
import nez.lang.GrammarFile;
import nez.util.ConsoleUtils;
import nez.util.StringUtils;
import nez.util.UList;

public class LCcheck extends Command {
	@Override
	public String getDesc() {
		return "a grammar checker";
	}

	@Override
	public void exec(CommandContext conf) {
		conf.getNezOption().setOption("example", true);
		
		GrammarFile gfile = conf.getGrammarFile(true);
		Grammar g = conf.getGrammar();
		
		UList<String> unparsedInputs = new UList<String>(new String[4]);
		UList<String> unformatedInputs = new UList<String>(new String[4]);
		UList<String> mismatchedInputs = new UList<String>(new String[4]);
		
		int totalCount = 0, parsedCount = 0, formatCount = 0, matchCount = 0;
//		long consumed = 0;
//		long time = 0;

		while(conf.hasInputSource()) {
			totalCount++;
			SourceContext source = conf.nextInputSource();
			String urn = source.getResourceName();
			CommonTree node = g.parse(source);
			if(node == null) {
				ConsoleUtils.println(source.getSyntaxErrorMessage());
				unparsedInputs.add(urn);
				continue;
			}
			if(source.hasUnconsumed()) {
				ConsoleUtils.println(source.getUnconsumedMessage());
			}
			g.logProfiler();

			parsedCount++;
			String formatted = gfile.formatCommonTree(node);
			source = SourceContext.newStringSourceContext("(formatted)", 1, formatted);
			CommonTree node2 = g.parse(source);
			if(node2 == null) {
				ConsoleUtils.println(source.getSyntaxErrorMessage());
				unformatedInputs.add(urn);
				continue;
			}
			if(source.hasUnconsumed()) {
				ConsoleUtils.println(source.getUnconsumedMessage());
				unformatedInputs.add(urn);
				continue;
			}
			formatCount++;
			String formatted2 = gfile.formatCommonTree(node2);
			if(!formatted.equals(formatted2)) {
				ConsoleUtils.println("[FAILED] mismatched " + urn);
				mismatchedInputs.add(urn);
				continue;
			}
			matchCount++;
		}
		
		if(totalCount > 1){
			Verbose.println(
					totalCount + " files, " +
					parsedCount + " parsed, " +
					formatCount + " formatted, " +
					matchCount + " matched, " +
					StringUtils.formatParcentage(matchCount, totalCount) + "% matched.");
		}
		if(unparsedInputs.size() > 0) {
			Verbose.println("unparsed: " + unparsedInputs);
		}
		if(unformatedInputs.size() > 0) {
			Verbose.println("unformatted: " + unformatedInputs);
		}
		if(mismatchedInputs.size() > 0) {
			ConsoleUtils.exit(1, "mismatched: " + mismatchedInputs);
		}

	}
	
}
