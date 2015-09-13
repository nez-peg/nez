package nez.ext;

import nez.ast.AbstractTree;
import nez.ast.AbstractTreeWriter;
import nez.io.SourceContext;
import nez.main.CommandContext;

public class Cxml extends Cparse {
	@Override
	public final String getDesc() {
		return "an XML converter";
	}

	@Override
	protected void makeOutputFile(CommandContext config, SourceContext source, AbstractTree<?> node) {
		AbstractTreeWriter w = new AbstractTreeWriter(config.getOption(), config.getOutputFileName(source, "ast"));
		w.writeTree(node);
		w.writeNewLine();
		w.close();
	}

}
