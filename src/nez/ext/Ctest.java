package nez.ext;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import nez.Grammar;
import nez.Parser;
import nez.lang.Example;
import nez.lang.GrammarFile;
import nez.main.Command;
import nez.main.CommandContext;
import nez.util.ConsoleUtils;

public class Ctest extends Command {
	@Override
	public void exec(CommandContext config) throws IOException {
		Grammar g = config.newGrammar();
		if (g instanceof GrammarFile) {
			test(config, (GrammarFile) g);
		}
	}

	private void test(CommandContext config, GrammarFile g) {
		List<Example> exampleList = g.getExampleList();
		if (exampleList != null) {
			HashMap<String, Parser> parserMap = new HashMap<>();
			long t1 = System.nanoTime();
			int total = 0, fail = 0;
			for (Example ex : exampleList) {
				String name = ex.getName();
				Parser p = parserMap.get(name);
				if (p == null) {
					p = g.newParser(name, config.getStrategy());
					if (p == null) {
						ConsoleUtils.println(ex.formatWarning("undefined nonterminal: " + name));
						continue;
					}
					parserMap.put(name, p);
				}
				if (ex.hasHash()) {
					total++;
				}
				if (!ex.test(p)) {
					fail++;
				}
			}
			long t2 = System.nanoTime();
			ConsoleUtils.println("Elapsed time (Example Tests): " + ((t2 - t1) / 1000000) + "ms");
			if (total > 0) {
				ConsoleUtils.println("Failure: " + fail + "/" + total + " Pass ratio: " + ((total - fail) * 100 / total) + "%");
			}
		}
	}

}
