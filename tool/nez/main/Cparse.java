package nez.main;

import java.io.IOException;

import nez.ast.Source;
import nez.ast.Tree;
import nez.parser.Parser;
import nez.tool.ast.TreeWriter;

public class Cparse extends Command {
	@Override
	public void exec() throws IOException {
		checkInputSource();
		Parser parser = newParser();
		parser.setDisabledUnconsumed(true);
		TreeWriter tw = this.getTreeWriter("ast xml json", "line");
		while (hasInputSource()) {
			Source input = nextInputSource();
			Tree<?> node = parser.parse(input);
			if (node == null) {
				parser.showErrors();
				continue;
			}
			// if (node != null) {
			// //record(parser.getProfiler(), node);
			// parser.logProfiler();
			if (this.outputDirectory != null) {
				tw.init(getOutputFileName(input, tw.getFileExtension()));
			}
			tw.writeTree(node);
			// }
		}
	}

	// private void record(ParserProfier prof, Tree<?> node) {
	// if (prof != null) {
	// System.gc();
	// prof.setCount("O.Size", node.countSubNodes());
	// long t1 = System.nanoTime();
	// Tree<?> t = node.dup();
	// long t2 = System.nanoTime();
	// ParserProfier.recordLatencyMS(prof, "O.Overhead", t1, t2);
	// }
	// }

}
