package nez.ext;

import nez.ast.AbstractTree;
import nez.ast.AbstractTreeUtils;
import nez.io.SourceContext;
import nez.main.CommandContext;
import nez.util.ConsoleUtils;

public class Cmd5 extends Cparse {

	@Override
	protected void makeOutputFile(CommandContext config, SourceContext source, AbstractTree<?> node) {
		ConsoleUtils.println(source.getResourceName() + ": " + AbstractTreeUtils.digestString(node));
	}
}
