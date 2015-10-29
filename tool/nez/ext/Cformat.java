package nez.ext;

import java.io.IOException;

import nez.Grammar;
import nez.Parser;
import nez.ast.Tree;
import nez.io.SourceContext;
import nez.lang.NezGrammar1;
import nez.lang.util.NezFileFormatter;
import nez.main.Command;
import nez.main.CommandContext;
import nez.util.ConsoleUtils;

public class Cformat extends Command {
	@Override
	public void exec(CommandContext config) throws IOException {
		Grammar g = new Grammar("nez");
		Parser p = new NezGrammar1().load(g, "File").newParser(config.getStrategy());
		SourceContext source = SourceContext.newFileContext(config.getGrammarPath());
		Tree<?> node = p.parseCommonTree(source);
		if (node == null) {
			ConsoleUtils.println(source.getSyntaxErrorMessage());
		}
		NezFileFormatter fmt = new NezFileFormatter();
		fmt.parse(node);
	}
}
