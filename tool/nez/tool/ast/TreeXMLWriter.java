package nez.tool.ast;

import nez.ast.Symbol;
import nez.ast.Tree;
import nez.util.StringUtils;

public class TreeXMLWriter extends TreeWriter {
	public TreeXMLWriter() {
		super(".xml");
	}

	@Override
	public final void writeTree(Tree<?> node) {
		String tag = node.getTag().toString();
		writeXML(null, tag, node);
		file.writeNewLine();
	}

	public final void writeXML(Symbol label, String tag, Tree<?> node) {
		file.writeIndent("<" + tag);
		if (label != null) {
			file.write(" label=\"" + label + "\"");
		}
		if (node.size() == 0) {
			String s = node.toText();
			if (s.equals("")) {
				file.write("/>");
			} else {
				if (!this.dataOption) {
					file.write(" pos=\"" + node.getSourcePosition() + "\"");
					file.write(" line=\"" + node.getLineNum() + "\"");
					file.write(" column=\"" + node.getColumn() + "\"");
				}
				file.write(">");
				file.write(node.toText());
				file.write("</" + tag + ">");
			}
			return;
		}
		for (int i = 0; i < node.size(); i++) {
			Tree<?> sub = node.get(i);
			String stag = sub.getTag().toString();
			if (stag.startsWith("@")) {
				file.write(" ");
				file.write(stag.substring(1));
				file.write("=");
				file.write(StringUtils.quoteString('"', sub.toText(), '"'));
			}
		}
		file.write(">");
		file.incIndent();
		for (int i = 0; i < node.size(); i++) {
			Tree<?> sub = node.get(i);
			String stag = sub.getTag().toString();
			if (!stag.startsWith("@")) {
				this.writeXML(node.getLabel(i), stag, sub);
			}
		}
		file.decIndent();
		file.writeIndent("</" + tag + ">");
	}

}
