package nez.debugger;

import java.io.IOException;

import nez.parser.Parser;
import nez.parser.ParserStrategy;
import nez.util.ConsoleUtils;
import nez.util.UList;

public class DebugManager {
	private int index;
	public UList<String> inputFileLists;
	public String text = null;

	public DebugManager(UList<String> inputFileLists) {
		this.inputFileLists = inputFileLists;
		this.index = 0;
	}

	public DebugManager(String text) {
		this.text = text;
		this.index = 0;
	}

	public void exec(Parser peg, ParserStrategy option) {
		if (this.text != null) {
			DebugSourceContext sc = DebugSourceContext.newStringContext(this.text);
			this.parse(sc, peg, option);
			return;
		}
		while (this.index < this.inputFileLists.size()) {
			DebugSourceContext sc = this.nextInputSource();
			this.parse(sc, peg, option);
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

	private void parse(DebugSourceContext sc, Parser peg, ParserStrategy option) {
		throw new RuntimeException("FIXME");
		// boolean matched;
		// DebugVMInstruction pc;
		// long startPosition = sc.getPosition();
		// DebugVMCompiler c = new DebugVMCompiler(option);
		// CommonTreeTransducer treeTransducer = new CommonTreeTransducer();
		// sc.initContext();
		// pc = c.compile(peg.getParserGrammar()).getStartPoint();
		// NezDebugger debugger = new NezDebugger(peg.getParserGrammar(), pc,
		// sc, c);
		// matched = debugger.exec();
		// if (matched) {
		// ConsoleUtils.println("match!!");
		// sc.newTopLevelNode();
		// Object node = sc.getLeftObject();
		// if (node == null) {
		// node = treeTransducer.newNode(null, sc, startPosition,
		// sc.getPosition(), 0, null);
		// }
		// CommonTree tree = (CommonTree) treeTransducer.commit(node);
		// TreeWriter w = new TreeWriter();
		// w.writeTree(tree);
		// } else {
		// ConsoleUtils.println("unmatch!!");
		// }
	}
}
