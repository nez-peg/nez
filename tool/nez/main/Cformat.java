package nez.main;

import java.io.IOException;

import nez.ast.Source;
import nez.ast.Tree;
import nez.io.CommonSource;
import nez.parser.Parser;
import nez.tool.ast.NezFileFormatter;

public class Cformat extends Command {
	@Override
	public void exec() throws IOException {
		Parser nezParser = getNezParser();
		Source source = CommonSource.newFileSource(newGrammar().getURN());
		Tree<?> node = nezParser.parse(source);
		nezParser.ensureNoErrors();
		NezFileFormatter fmt = new NezFileFormatter();
		fmt.parse(node);
	}
}
