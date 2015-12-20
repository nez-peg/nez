package nez.tool.ast;

import nez.ast.Symbol;
import nez.ast.Tree;
import nez.util.FileBuilder;
import nez.util.StringUtils;

public class TreeWriter {
	protected FileBuilder file = new FileBuilder();
	protected boolean dataOption = false;

	public void init(String path) {
		file.close();
		file = new FileBuilder(path);
	}

	public void setDataOption(boolean dataOption) {
		this.dataOption = dataOption;
	}

	public void writeTree(Tree<?> node) {
		writeTree(null, node);
		file.writeNewLine();
		file.flush();
	}

	private void writeTree(Symbol label, Tree<?> node) {
		if (node == null) {
			file.writeIndent("null");
			return;
		}
		if (label == null) {
			file.writeIndent("#" + node.getTag().toString() + "[");
		} else {
			file.writeIndent("$" + label + " #" + node.getTag() + "[");
		}
		if (node.size() == 0) {
			file.write(StringUtils.quoteString('\'', node.toText(), '\''));
			file.write("]");
		} else {
			file.incIndent();
			for (int i = 0; i < node.size(); i++) {
				this.writeTree(node.getLabel(i), node.get(i));
			}
			file.decIndent();
			file.writeIndent("]");
		}
	}

	public String getFileExtension() {
		return "ast";
	}

	// public void writeTag(Tree<?> node) {
	// TreeMap<String, Integer> m = new TreeMap<String, Integer>();
	// this.countTag(node, m);
	// for (String k : m.keySet()) {
	// this.write("#" + k + ":" + m.get(k));
	// }
	// this.writeNewLine();
	// }
	//
	// private void countTag(Tree<?> node, TreeMap<String, Integer> m) {
	// for (int i = 0; i < node.size(); i++) {
	// countTag(node.get(i), m);
	// }
	// String key = node.getTag().toString();
	// Integer n = m.get(key);
	// if (n == null) {
	// m.put(key, 1);
	// } else {
	// m.put(key, n + 1);
	// }
	// }

}
