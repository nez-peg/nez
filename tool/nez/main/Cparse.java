package nez.main;

import java.io.IOException;

import nez.ast.Source;
import nez.ast.Tree;
import nez.ast.TreeUtils;
import nez.ast.TreeWriter;
import nez.parser.Parser;
import nez.util.ConsoleUtils;

public class Cparse extends Command {
	@Override
	public void exec() throws IOException {
		checkInputSource();
		Parser parser = newParser();
		makeOutputFile(null, null); // check format
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
			makeOutputFile(input, node);
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

	protected void makeOutputFile(Source source, Tree<?> node) {
		if (outputFormat == null) {
			outputFormat = "ast";
		}
		switch (outputFormat) {
		case "ast":
			if (node != null) {
				TreeWriter w = new TreeWriter(strategy, getOutputFileName(source, "ast"));
				w.writeTree(node);
				w.writeNewLine();
				w.close();
			}
			break;
		case "xml":
			if (node != null) {
				TreeWriter w = new TreeWriter(strategy, getOutputFileName(source, "xml"));
				w.writeXML(node);
				w.writeNewLine();
				w.close();
			}
			break;
		case "json":
			if (node != null) {
				TreeWriter w = new TreeWriter(strategy, getOutputFileName(source, "json"));
				w.writeJSON(node);
				w.writeNewLine();
				w.close();
			}
			break;
		case "md5":
			if (node != null) {
				ConsoleUtils.println(source.getResourceName() + ": " + TreeUtils.digestString(node));
			}
			break;
		case "none": {
			break;
		}
		default:
			Extension e = (Extension) this.newExtendedOutputHandler("");
			if (node != null) {
				e.makeOutputFile(node, getOutputFileName(source, null));
			}
		}
	}

	public abstract static class Extension {
		public abstract void makeOutputFile(Tree<?> node, String path);
	}

}
