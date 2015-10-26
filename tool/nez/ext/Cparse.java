package nez.ext;

import java.io.IOException;

import nez.Parser;
import nez.ast.Tree;
import nez.ast.TreeWriter;
import nez.io.SourceContext;
import nez.main.Command;
import nez.main.CommandContext;
import nez.main.NezProfier;
import nez.util.ConsoleUtils;

public class Cparse extends Command {
	@Override
	public void exec(CommandContext config) throws IOException {
		Parser g = config.newParser();
		while (config.hasInput()) {
			SourceContext input = config.nextInput();
			Tree<?> node = g.parseCommonTree(input);
			if (node == null) {
				ConsoleUtils.println(input.getSyntaxErrorMessage());
				continue;
			}
			if (input.hasUnconsumed()) {
				ConsoleUtils.println(input.getUnconsumedMessage());
			}
			record(g.getProfiler(), node);
			g.logProfiler();
			makeOutputFile(config, input, node);
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

	protected void makeOutputFile(CommandContext config, SourceContext source, Tree<?> node) {
		TreeWriter w = new TreeWriter(config.getStrategy(), config.getOutputFileName(source, "ast"));
		w.writeTree(node);
		w.writeNewLine();
		w.close();
	}
}
