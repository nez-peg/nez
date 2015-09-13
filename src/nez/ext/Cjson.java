package nez.ext;

import nez.SourceContext;
import nez.ast.AbstractTree;
import nez.ast.AbstractTreeWriter;
import nez.main.CommandContext;

public class Cjson extends Cparse {
	@Override
	public final String getDesc() {
		return "a JSON converter";
	}

	@Override
	protected void makeOutputFile(CommandContext config, SourceContext source, AbstractTree<?> node) {
		AbstractTreeWriter w = new AbstractTreeWriter(config.getOption(), config.getOutputFileName(source, "json"));
		w.writeTree(node);
		w.writeNewLine();
		w.close();
	}
}
