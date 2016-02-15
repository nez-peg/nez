package nez.lang.ast;

import java.util.List;

import nez.ast.Tree;
import nez.lang.Grammar;
import nez.util.UList;

public class GrammarExample {
	private UList<Example> exampleList;

	public GrammarExample(Grammar grammar) {
		exampleList = new UList<Example>(new Example[8]);
	}

	public List<Example> getExampleList() {
		return exampleList;
	}

	public final void addExample(boolean isPublic, Tree<?> nameNode, String hash, Tree<?> textNode) {
		exampleList.add(new Example(isPublic, nameNode, hash, textNode));
	}

	public final static void add(Grammar g, boolean isPublic, Tree<?> nameNode, String hash, Tree<?> textNode) {
		GrammarExample example = (GrammarExample) g.getMetaData("example");
		if (example == null) {
			example = new GrammarExample(g);
			g.setMetaData("example", example);
		}
		example.addExample(isPublic, nameNode, hash, textNode);
	}

	public final List<String> getExampleList(String name) {
		UList<String> l = new UList<String>(new String[4]);
		if (exampleList != null) {
			for (Example ex : exampleList) {
				if (name.equals(ex.getName())) {
					l.add(ex.getText());
				}
			}
		}
		return l;
	}

	public static class Example {
		public final boolean isPublic;
		public final Tree<?> nameNode;
		public final Tree<?> textNode;
		public final String hash;

		public Example(boolean isPublic, Tree<?> nameNode, String hash, Tree<?> textNode) {
			this.isPublic = isPublic;
			this.nameNode = nameNode;
			this.textNode = textNode;
			this.hash = hash;
		}

		public final String getName() {
			return nameNode.toText();
		}

		public final String getText() {
			return textNode.toText();
		}

		public final boolean hasHash() {
			return this.hash != null;
		}

		public String formatPanic(String msg) {
			return nameNode.formatSourceMessage("panic", msg);
		}

		public String formatWarning(String msg) {
			return nameNode.formatSourceMessage("warning", msg);
		}

	}

}
