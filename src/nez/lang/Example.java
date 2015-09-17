package nez.lang;

import nez.Parser;
import nez.ast.AbstractTree;
import nez.ast.AbstractTreeUtils;
import nez.io.SourceContext;
import nez.util.ConsoleUtils;

public class Example {
	boolean isPublic;
	AbstractTree<?> nameNode;
	AbstractTree<?> textNode;
	String hash;

	public Example(boolean isPublic, AbstractTree<?> nameNode, String hash, AbstractTree<?> textNode) {
		this.isPublic = true;
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

	public String formatWarning(String msg) {
		return nameNode.formatSourceMessage("warning", msg);
	}

	public boolean test(Parser p) {
		SourceContext source = textNode.newSourceContext();
		String name = nameNode.toText() + " (" + textNode.getSource().getResourceName() + ":" + textNode.getLinenum() + ")";
		AbstractTree<?> node = p.parseCommonTree(source);
		if (node == null) {
			ConsoleUtils.println("[FAIL] " + name);
			ConsoleUtils.println(source.getSyntaxErrorMessage());
			return false;
		}
		String nodehash = AbstractTreeUtils.digestString(node);
		if (hash == null) {
			ConsoleUtils.println("[HASH] " + name + " ~" + nodehash);
			ConsoleUtils.println("   ", this.getText());
			ConsoleUtils.println("---");
			ConsoleUtils.println("   ", node);
			// ConsoleUtils.println(node);
			this.hash = nodehash;
			return true;
		}
		if (nodehash.startsWith(hash)) {
			ConsoleUtils.println("[PASS] " + name + " ~" + nodehash);
			return true;
		}
		ConsoleUtils.println("[FAIL] " + name + " ~" + nodehash + node + "\n");
		ConsoleUtils.println("   ", this.getText());
		ConsoleUtils.println("---");
		ConsoleUtils.println("   ", node);
		return false;
	}

}
