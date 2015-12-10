package nez.ext;

import java.io.IOException;

import nez.ast.Tree;
import nez.io.SourceStream;
import nez.lang.Grammar;
import nez.lang.NezGrammar1;
import nez.lang.util.NezFileFormatter;
import nez.main.Command;
import nez.main.CommandContext;
import nez.parser.Parser;

public class Cformat extends Command {
	@Override
	public void exec(CommandContext config) throws IOException {
		Grammar grammar = new Grammar("nez");
		Parser parser = new NezGrammar1().load(grammar, "File").newParser(config.getStrategy());
		SourceStream source = SourceStream.newFileContext(config.getGrammarPath());
		Tree<?> node = parser.parseCommonTree(source);
		parser.ensureNoErrors();
		NezFileFormatter fmt = new NezFileFormatter();
		fmt.parse(node);
	}
}
