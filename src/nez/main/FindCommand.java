package nez.main;

import java.io.IOException;

import nez.NameSpace;
import nez.Grammar2;
import nez.SourceContext;
import nez.expr.GrammarChecker;
import nez.expr.NezParser;
import nez.expr.Production;
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
		UList<Grammar2> pList = load(config.getInputFileList());
		while( (text = ConsoleUtils.readMultiLine(">>> ", "    ")) != null) {
			ConsoleUtils.println(text);
			for(Grammar2 p: pList) {
				if(p.match(text)) {
					ConsoleUtils.println(p.getStartRule().getLocalName());
				}
			}
		}
		
	}

	UList<Grammar2> load(UList<String> fileList) {
		UList<Grammar2> pList = new UList<Grammar2>(new Grammar2[fileList.size()*2]);
		Verbose.print("Loading ..");
		try {
			for(String f : fileList) {
				NameSpace g = NameSpace.load(f);
				UList<Production> rules = g.getDefinedRuleList();
				for(Production r : rules) {
					if(r.isPublic()) {
						Grammar2 p = g.newProduction(r.getLocalName());
						p.compile();
						pList.add(p);
						Verbose.print(" " + r.getUniqueName());
					}
				}
			}
		}
		catch(IOException e) {
			ConsoleUtils.exit(1, e.getMessage());
		}
		Verbose.println(" " + pList.size() + " rules");
		return pList;
	}
	
}
