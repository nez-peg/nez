package nez.ext;

import java.io.IOException;

import nez.ast.Source;
import nez.ast.Tree;
import nez.io.CommonSource;
import nez.lang.Grammar;
import nez.lang.ast.NezGrammarCombinator;
import nez.lang.util.NezFileFormatter;
import nez.main.Command;
import nez.parser.Parser;

public class Cformat extends Command {
	@Override
	public void exec(CommandContext config) throws IOException {
		Grammar grammar = new Grammar("nez");
		Parser parser = new NezGrammarCombinator().load(grammar, "File").newParser(config.getStrategy());
		Source source = CommonSource.newFileSource(config.getGrammarPath());
		Tree<?> node = parser.parse(source);
		parser.ensureNoErrors();
		NezFileFormatter fmt = new NezFileFormatter();
		fmt.parse(node);
	}
}
