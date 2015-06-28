package nez.main;

import nez.SourceContext;
import nez.ast.CommonTree;
import nez.ast.CommonTreeWriter;
import nez.lang.Grammar;
import nez.util.ConsoleUtils;

public class LCast extends Command {
	@Override
	public String getDesc() {
		return "AST parser";
	}

	@Override
	public void exec(CommandContext config) {
		Grammar p = config.getGrammar();
		while(config.hasInput()) {
			SourceContext file = config.getInputSourceContext();
			CommonTree node = p.parse(file);
			if(node == null) {
				ConsoleUtils.println(file.getSyntaxErrorMessage());
				continue;
			}
			if(file.hasUnconsumed()) {
				ConsoleUtils.println(file.getUnconsumedMessage());
			}
			file = null;
			record(p.getProfiler(), node);
			p.logProfiler();
			new CommonTreeWriter().transform(config.getOutputFileName(file), node);
		}
	}
	
	private void record(NezProfier prof, CommonTree node) {
		if(prof != null) {
			System.gc();
			prof.setCount("O.Size", node.count());
			long t1 = System.nanoTime();
			CommonTree t = node.dup();
			long t2 = System.nanoTime();
			NezProfier.recordLatencyMS(prof, "O.Overhead", t1, t2);
		}
	}

}


