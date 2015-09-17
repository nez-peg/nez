package nez.ext;

import nez.ast.AbstractTree;
import nez.ast.AbstractTreeWriter;
import nez.io.SourceContext;
import nez.main.CommandContext;

public class Cjson extends Cparse {
	@Override
	protected void makeOutputFile(CommandContext config, SourceContext source, AbstractTree<?> node) {
		AbstractTreeWriter w = new AbstractTreeWriter(config.getStrategy(), config.getOutputFileName(source, "json"));
		w.writeTree(node);
		w.writeNewLine();
		w.close();
	}
}
