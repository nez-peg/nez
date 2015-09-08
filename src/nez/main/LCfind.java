package nez.main;

import java.io.IOException;

import nez.NezOption;
import nez.lang.Parser;
import nez.lang.GrammarFile;
import nez.lang.Production;
import nez.util.ConsoleUtils;
import nez.util.UList;

public class LCfind extends Command {

	@Override
	public String getDesc() {
		return "locate nonterminals that match the input";
	}

	@Override
	public void exec(CommandContext config) {
		String text = null;
		UList<Parser> pList = load(config.getInputFileList());
		while ((text = ConsoleUtils.readMultiLine(">>> ", "    ")) != null) {
			ConsoleUtils.println(text);
			for (Parser p : pList) {
				if (p.match(text)) {
					ConsoleUtils.println(p.getStartProduction().getLocalName());
				}
			}
		}

	}

	UList<Parser> load(UList<String> fileList) {
		UList<Parser> pList = new UList<Parser>(new Parser[fileList.size() * 2]);
		Verbose.print("Loading ..");
		try {
			for (String f : fileList) {
				GrammarFile g = GrammarFile.loadNezFile(f, NezOption.newDefaultOption());
				UList<Production> rules = g.getDefinedRuleList();
				for (Production r : rules) {
					if (r.isPublic()) {
						Parser p = g.newGrammar(r.getLocalName());
						p.compile();
						pList.add(p);
						Verbose.print(" " + r.getUniqueName());
					}
				}
			}
		} catch (IOException e) {
			ConsoleUtils.exit(1, e.getMessage());
		}
		Verbose.println(" " + pList.size() + " rules");
		return pList;
	}

}
