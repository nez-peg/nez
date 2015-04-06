package nez.main;

import java.io.IOException;

import nez.Grammar;
import nez.Production;
import nez.SourceContext;
import nez.expr.GrammarChecker;
import nez.expr.NezParser;
import nez.expr.Rule;
import nez.util.ConsoleUtils;
import nez.util.UList;

public class FindCommand extends Command {

	@Override
	public String getDesc() {
		return "locate nonterminals that match the input";
	}

	@Override
	public void exec(CommandConfigure config) {
		String text = null;
		UList<Production> pList = load(config.getInputFileList());
		while( (text = ConsoleUtils.readMultiLine(">>>", "   ")) != null) {
			ConsoleUtils.println(text);
			for(Production p: pList) {
				if(p.match(text)) {
					ConsoleUtils.println(p.getStartRule().getLocalName());
				}
			}
		}
		
	}

	UList<Production> load(UList<String> fileList) {
		UList<Production> pList = new UList<Production>(new Production[fileList.size()*2]);
		try {
			for(String f : fileList) {
				Grammar g = Grammar.load(f);
				UList<Rule> rules = g.getDefinedRuleList();
				for(Rule r : rules) {
					if(r.isPublic()) {
						Production p = g.newProduction(r.getLocalName());
						p.compile();
						pList.add(p);
					}
				}
			}
		}
		catch(IOException e) {
			ConsoleUtils.exit(1, e.getMessage());
		}
		return pList;
	}
	
}
