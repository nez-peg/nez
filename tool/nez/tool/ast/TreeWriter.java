package nez.tool.ast;

import nez.ast.Symbol;
import nez.ast.Tree;
import nez.util.FileBuilder;
import nez.util.StringUtils;

public abstract class TreeWriter {
	protected String fileExtension;
	protected FileBuilder file = new FileBuilder();
	protected boolean dataOption = false;

	public TreeWriter(String ext) {
		this.fileExtension = ext;
	}

	public final void init(String path) {
		file.close();
		file = new FileBuilder(path);
	}

	public final String getFileExtension() {
		return fileExtension;
	}

	public final void setDataOption(boolean dataOption) {
		this.dataOption = dataOption;
	}

	public abstract void writeTree(Tree<?> node);

	public static class LineWriter extends TreeWriter {
		public LineWriter() {
			super(".out");
		}

		@Override
		public void writeTree(Tree<?> node) {
			file.write(node == null ? "null" : node.toString());
			file.writeNewLine();
			file.flush();
		}
	}

	public static class AstWriter extends TreeWriter {
		public AstWriter() {
			super(".ast");
		}

		@Override
		public void writeTree(Tree<?> node) {
			if (node != null) {
				writeTree(null, node);
			}
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
				file.writeIndent("$" + label + "=#" + node.getTag() + "[");
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

	}

	public static class DigestWriter extends TreeWriter {
		public DigestWriter() {
			super(".md5");
		}

		@Override
		public void writeTree(Tree<?> node) {
			file.write(node == null ? "null" : node.toString());
			file.writeNewLine();
			file.flush();
		}
	}

}
