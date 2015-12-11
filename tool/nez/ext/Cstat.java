package nez.ext;

import java.io.IOException;

import nez.lang.Grammar;
import nez.lang.Production;
import nez.main.Command;
import nez.parser.Parser;
import nez.util.ConsoleUtils;

public class Cstat extends Command {
	@Override
	public void exec(CommandContext config) throws IOException {
		Grammar g = config.newGrammar();
		Parser p = g.newParser(config.getStrategy());
		Grammar gg = p.getParserGrammar();
		int base = gg.size();
		int c = 0;
		for (Production pp : gg) {
			String uname = pp.getLocalName();
			if (uname.indexOf('!') > 0) {
				c++;
			}
		}
		ConsoleUtils.println("Grammars: " + base + " => " + (base + c));
	}
}
