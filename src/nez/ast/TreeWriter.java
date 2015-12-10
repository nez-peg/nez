package nez.ast;

import java.util.TreeMap;

import nez.parser.ParserStrategy;
import nez.util.FileBuilder;
import nez.util.StringUtils;

public class TreeWriter extends FileBuilder {

	boolean dataOption = true;

	public TreeWriter() {
		super(null);
	}

	public TreeWriter(String path) {
		super(path);
	}

	public TreeWriter(ParserStrategy option, String path) {
		super(path);
	}

	public TreeWriter(String path, String dir, String ext) {
		this(StringUtils.toFileName(path, dir, ext));
	}

	public TreeWriter(ParserStrategy option, String path, String dir, String ext) {
		this(StringUtils.toFileName(path, dir, ext));
	}

	public final void writeTree(Tree<?> node) {
		if (node == null) {
			this.writeIndent("null");
			return;
		}
		this.writeIndent("#" + node.getTag().toString() + "[");
		if (node.size() == 0) {
			this.write(StringUtils.quoteString('\'', node.toText(), '\''));
			this.write("]");
		} else {
			this.incIndent();
			for (int i = 0; i < node.size(); i++) {
				this.writeTree(node.get(i));
			}
			this.decIndent();
			this.writeIndent("]");
		}
	}

	public final void writeXML(Tree<?> node) {
		if (node.size() == 2 && node.getTag() == Symbol.MetaSymbol) {
			writeXML(node.get(0).toText(), node.get(1));
		} else {
			String tag = node.getTag().toString();
			writeXML(tag, node);
		}
	}

	public final void writeXML(String tag, Tree<?> node) {
		this.writeIndent("<" + tag);
		if (node.size() == 0) {
			String s = node.toText();
			if (s.equals("")) {
				this.write("/>");
			} else {
				this.write(">");
				this.write(node.toText());
				this.write("</" + tag + ">");
			}
		} else {
			for (int i = 0; i < node.size(); i++) {
				Tree<?> sub = node.get(i);
				String stag = sub.getTag().toString();
				if (stag.startsWith("@")) {
					this.write(" ");
					this.write(stag.substring(1));
					this.write("=");
					this.write(StringUtils.quoteString('"', sub.toText(), '"'));
				}
			}
			this.write(">");
			this.incIndent();
			for (int i = 0; i < node.size(); i++) {
				Tree<?> sub = node.get(i);
				String stag = sub.getTag().toString();
				if (!stag.startsWith("@")) {
					this.writeXML(sub);
				}
			}
			this.decIndent();
			this.writeIndent("</" + tag + ">");
		}
	}

	public void writeJSON(Tree<?> node) {
		if (node.size() == 0) {
			if (dataOption) {
				this.write(StringUtils.quoteString('"', node.toText(), '"'));
			} else {
				this.write("{");
				this.write("\"#\":");
				this.write(StringUtils.quoteString('"', node.getTag().toString(), '"'));
				this.write(",\"linenum\":");
				this.write("" + node.getLineNum());
				this.write(",\"column\":");
				this.write("" + node.getColumn());
				this.write(",\"text\":");
				this.write(StringUtils.quoteString('"', node.toText(), '"'));
				this.write("}");
			}
			return;
		}
		if (node.isAllLabeled()) {
			this.write("{");
			for (int i = 0; i < node.size(); i++) {
				if (i > 0) {
					this.write(",");
				}
				this.write(StringUtils.quoteString('"', node.getLabel(i).toString(), '"'));
				this.write(":");
				writeJSON(node.get(i));
			}
			if (!dataOption) {
				this.write(",\"#\":");
				this.write(StringUtils.quoteString('"', node.getTag().toString(), '"'));
			}
			this.write("}");
			return;
		}
		if (!dataOption) {
			this.write("{");
			this.write("\"#\":");
			this.write(StringUtils.quoteString('"', node.getTag().toString(), '"'));
			this.write(",\"list\":");
		}
		this.write("[");
		for (int i = 0; i < node.size(); i++) {
			if (i > 0) {
				this.write(",");
			}
			writeJSON(node.get(i));
		}
		this.write("]");
		if (!dataOption) {
			this.write("}");
		}
	}

	public void writeTag(Tree<?> node) {
		TreeMap<String, Integer> m = new TreeMap<String, Integer>();
		this.countTag(node, m);
		for (String k : m.keySet()) {
			this.write("#" + k + ":" + m.get(k));
		}
		this.writeNewLine();
	}

	private void countTag(Tree<?> node, TreeMap<String, Integer> m) {
		for (int i = 0; i < node.size(); i++) {
			countTag(node.get(i), m);
		}
		String key = node.getTag().toString();
		Integer n = m.get(key);
		if (n == null) {
			m.put(key, 1);
		} else {
			m.put(key, n + 1);
		}
	}

}
