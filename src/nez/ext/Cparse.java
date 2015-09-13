package nez.ext;

import java.io.IOException;

import nez.Parser;
import nez.SourceContext;
import nez.ast.AbstractTree;
import nez.ast.AbstractTreeWriter;
import nez.main.Command;
import nez.main.CommandContext;
import nez.main.NezProfier;
import nez.util.ConsoleUtils;

public class Cparse extends Command {
	@Override
	public String getDesc() {
		return "an AST parser";
	}

	@Override
	public void exec(CommandContext config) throws IOException {
		Parser g = config.newParser();
		while (config.hasInput()) {
			SourceContext input = config.nextInput();
			AbstractTree<?> node = g.parseCommonTree(input);
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

	private void record(NezProfier prof, AbstractTree<?> node) {
		if (prof != null) {
			System.gc();
			prof.setCount("O.Size", node.countSubNodes());
			long t1 = System.nanoTime();
			AbstractTree<?> t = node.dup();
			long t2 = System.nanoTime();
			NezProfier.recordLatencyMS(prof, "O.Overhead", t1, t2);
		}
	}

	protected void makeOutputFile(CommandContext config, SourceContext source, AbstractTree<?> node) {
		AbstractTreeWriter w = new AbstractTreeWriter(config.getOption(), config.getOutputFileName(source, "ast"));
		w.writeTree(node);
		w.writeNewLine();
		w.close();
	}
}
