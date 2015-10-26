package nez.ext;

import nez.ast.Tree;
import nez.ast.TreeUtils;
import nez.io.SourceContext;
import nez.main.CommandContext;
import nez.util.ConsoleUtils;

public class Cmd5 extends Cparse {

	@Override
	protected void makeOutputFile(CommandContext config, SourceContext source, Tree<?> node) {
		ConsoleUtils.println(source.getResourceName() + ": " + TreeUtils.digestString(node));
	}
}
