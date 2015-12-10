package nez.ext;

import nez.ast.Tree;
import nez.ast.TreeWriter;
import nez.io.SourceStream;
import nez.main.CommandContext;

public class Cxml extends Cparse {
	@Override
	protected void makeOutputFile(CommandContext config, SourceStream source, Tree<?> node) {
		TreeWriter w = new TreeWriter(config.getStrategy(), config.getOutputFileName(source, "ast"));
		w.writeTree(node);
		w.writeNewLine();
		w.close();
	}

}
