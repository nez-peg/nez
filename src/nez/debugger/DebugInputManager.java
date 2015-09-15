package nez.debugger;

import java.io.IOException;

import nez.NezOption;
import nez.Parser;
import nez.ast.AbstractTreeWriter;
import nez.ast.CommonTree;
import nez.ast.CommonTreeTransducer;
import nez.util.ConsoleUtils;
import nez.util.UList;

public class DebugInputManager {
	private int index;
	public UList<String> inputFileLists;

	public DebugInputManager(UList<String> inputFileLists) {
		this.inputFileLists = inputFileLists;
		this.index = 0;
	}

	public void exec(Parser peg, NezOption option) {
		while (this.index < this.inputFileLists.size()) {
			DebugSourceContext sc = this.nextInputSource();
			boolean matched;
			DebugVMInstruction pc;
			long startPosition = sc.getPosition();
			DebugVMCompiler c = new DebugVMCompiler(option);
			CommonTreeTransducer treeTransducer = new CommonTreeTransducer();
			sc.initContext();
			pc = c.compile(peg.getGrammar()).getStartPoint();
			NezDebugger debugger = new NezDebugger(peg.getGrammar(), pc, sc, c);
			matched = debugger.exec();
			if (matched) {
				ConsoleUtils.println("match!!");
				sc.newTopLevelNode();
				Object node = sc.getLeftObject();
				if (node == null) {
					node = treeTransducer.newNode(null, sc, startPosition, sc.getPosition(), 0, null);
				}
				CommonTree tree = (CommonTree) treeTransducer.commit(node);
				AbstractTreeWriter w = new AbstractTreeWriter();
				w.writeTree(tree);
				w.writeNewLine();
				w.close();
			} else {
				ConsoleUtils.println("unmatch!!");
			}
		}
	}

	public final DebugSourceContext nextInputSource() {
		if (this.index < this.inputFileLists.size()) {
			String f = this.inputFileLists.ArrayValues[this.index];
			this.index++;
			try {
				return DebugSourceContext.newDebugFileContext(f);
			} catch (IOException e) {
				ConsoleUtils.exit(1, "cannot open: " + f);
			}
		}
		ConsoleUtils.exit(1, "error: input file list is empty");
		return null;
	}
}
