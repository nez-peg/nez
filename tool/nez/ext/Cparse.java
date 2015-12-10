package nez.ext;

import java.io.IOException;

import nez.NezProfier;
import nez.ast.Tree;
import nez.ast.TreeWriter;
import nez.io.SourceStream;
import nez.main.Command;
import nez.main.CommandContext;
import nez.parser.Parser;

public class Cparse extends Command {
	@Override
	public void exec(CommandContext config) throws IOException {
		Parser parser = config.newParser();
		parser.setDisabledUnconsumed(true);
		while (config.hasInput()) {
			SourceStream input = config.nextInput();
			Tree<?> node = parser.parseCommonTree(input);
			if (node != null) {
				record(parser.getProfiler(), node);
				parser.logProfiler();
				makeOutputFile(config, input, node);
			}
		}
	}

	private void record(NezProfier prof, Tree<?> node) {
		if (prof != null) {
			System.gc();
			prof.setCount("O.Size", node.countSubNodes());
			long t1 = System.nanoTime();
			Tree<?> t = node.dup();
			long t2 = System.nanoTime();
			NezProfier.recordLatencyMS(prof, "O.Overhead", t1, t2);
		}
	}

	protected void makeOutputFile(CommandContext config, SourceStream source, Tree<?> node) {
		TreeWriter w = new TreeWriter(config.getStrategy(), config.getOutputFileName(source, "ast"));
		w.writeTree(node);
		w.writeNewLine();
		w.close();
	}
}
