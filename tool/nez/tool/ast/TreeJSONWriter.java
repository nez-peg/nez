package nez.tool.ast;

import nez.ast.Tree;
import nez.util.StringUtils;

public class TreeJSONWriter extends TreeWriter {

	public TreeJSONWriter() {
		super(".json");
	}

	@Override
	public void writeTree(Tree<?> node) {
		writeJSON(node);
		file.writeNewLine();
	}

	private void writeJSON(Tree<?> node) {
		if (node.size() == 0) {
			String text = node.toText();
			if (dataOption) {
				try {
					Double v = Double.parseDouble(text);
					file.write(v.toString());
					return;
				} catch (NumberFormatException e) {
				}
				try {
					Long v = Long.parseLong(text);
					file.write(v.toString());
					return;
				} catch (NumberFormatException e) {
				}
				file.write(StringUtils.quoteString('"', text, '"'));
			} else {
				file.write("{");
				file.write("\"type\":");
				file.write(StringUtils.quoteString('"', node.getTag().toString(), '"'));
				file.write(",\"pos\":");
				file.write("" + node.getSourcePosition());
				file.write(",\"line\":");
				file.write("" + node.getLineNum());
				file.write(",\"column\":");
				file.write("" + node.getColumn());
				file.write(",\"text\":");
				file.write(StringUtils.quoteString('"', text, '"'));
				file.write("}");
			}
			return;
		}
		if (node.isAllLabeled()) {
			file.write("{");
			if (!dataOption) {
				file.write("\"type\":");
				file.write(StringUtils.quoteString('"', node.getTag().toString(), '"'));
				file.write(",");
			}
			for (int i = 0; i < node.size(); i++) {
				if (i > 0) {
					file.write(",");
				}
				file.write(StringUtils.quoteString('"', node.getLabel(i).toString(), '"'));
				file.write(":");
				writeJSON(node.get(i));
			}
			file.write("}");
			return;
		}
		file.write("[");
		for (int i = 0; i < node.size(); i++) {
			if (i > 0) {
				file.write(",");
			}
			writeJSON(node.get(i));
		}
		file.write("]");
	}

}
