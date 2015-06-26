package nez.fsharp;

import java.io.IOException;

import nez.SourceContext;
import nez.ast.CommonTree;
import nez.ast.CommonTreeWriter;
import nez.lang.Grammar;
import nez.main.Command;
import nez.main.CommandConfigure;
import nez.main.Recorder;
import nez.util.ConsoleUtils;

public class TypeCheckCommand extends Command {

	@Override
	public void exec(CommandConfigure config) {
		Recorder rec = config.getRecorder();
		Grammar p = config.getGrammar();
		p.record(rec);
		while(config.hasInput()) {
			SourceContext file = config.getInputSourceContext();
			file.start(rec);
//			CommonTree.gcCount = 0;
			ModifiableTree node = (ModifiableTree)(p.parse(file, new ModifiableTreeFactory()));
			file.done(rec);
			if(node == null) {
				ConsoleUtils.println(file.getSyntaxErrorMessage());
				continue;
			}
			if(file.hasUnconsumed()) {
				ConsoleUtils.println(file.getUnconsumedMessage());
			}
			file = null;
			if(rec != null) {
				record(rec, node);
				//rec.log();
			}
			//new CommonTreeWriter().transform(config.getOutputFileName(file), node);
			try {
				new FSharpWriter().writeTo(null, node);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public String getDesc() {
		// TODO Auto-generated method stub
		return null;
	}
	
	private void record(Recorder rec, ModifiableTree node) {
		rec.setCount("O.Size", node.count());
		System.gc();
		long t1 = System.nanoTime();
		ModifiableTree t = node.dup();
		long t2 = System.nanoTime();
		Recorder.recordLatencyMS(rec, "O.Overhead", t1, t2);
//		rec.setCount("O.GC", CommonTree.gcCount);
	}

}
