package nez.ast.script;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import nez.Parser;
import nez.ast.Tree;
import nez.ast.TreeVisitor;
import nez.ast.jcode.JCodeGenerator;
import nez.ast.jcode.JCodeTree;
import nez.ast.jcode.JCodeTreeTransducer;
import nez.io.SourceContext;
import nez.util.ConsoleUtils;

public class ScriptContext extends TreeVisitor {
	private Parser parser;
	private JCodeTreeTransducer treeTransducer;

	public ScriptContext(Parser parser) {
		this.parser = parser;
		this.treeTransducer = new JCodeTreeTransducer();
	}

	public void load(String path) throws IOException {
		eval(SourceContext.newFileContext(path));
	}

	public Object eval(String uri, int linenum, String script) {
		return eval(SourceContext.newStringContext(uri, linenum, script));
	}

	private Object eval(SourceContext source) {
		JCodeTree node = (JCodeTree) this.parser.parse(source, this.treeTransducer);
		if (node == null) {
			println(source.getSyntaxErrorMessage());
			return this; // nothing
		}
		Object result = null;
		for (JCodeTree sub : node) {
			result = this.visit("eval", sub);
		}
		return result;
	}

	@Override
	public Object visitUndefinedNode(Tree<?> node) {
		System.out.println("undefined: " + node);
		return null;
	}

	public void execute(JCodeTree node) {
		JCodeGenerator generator = new JCodeGenerator("GeneratedClass");
		generator.visit(node);
		Class<?> mainClass = generator.generateClass();
		try {
			System.out.println("\n@@@@ Execute Byte Code @@@@");
			Method method = mainClass.getMethod("main");
			method.invoke(null);
		} catch (NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			System.out.println("Invocation problem");
			e.printStackTrace();
		}
	}

	public final void println(Object o) {
		ConsoleUtils.println(o);
	}

	// @Override
	// public void exec(CommandContext config) throws IOException {
	// this.treeTransducer = ;
	// this.parser = config.newParser();
	// if (config.hasInput()) {
	// JCodeTree node = parse(config);
	// execute(node);
	// } else {
	// shell();
	// }
	// }
	//
	// public final JCodeTree eval(CommandContext config) throws IOException {
	// SourceContext source = config.nextInput();
	// JCodeTree node = (JCodeTree) this.parser.parse(source,
	// this.treeTransducer);
	// if (node == null) {
	// ConsoleUtils.println(source.getSyntaxErrorMessage());
	// }
	// System.out.println("parsed:\n" + node + "\n");
	// return node;
	// }
	//
	// public JCodeTree parse(String urn, int linenum, String text) {
	// SourceContext source = SourceContext.newStringSourceContext(urn, linenum,
	// text);
	// JCodeTree node = (JCodeTree) parser.parse(source, treeTransducer);
	// if (node == null) {
	// ConsoleUtils.println(source.getSyntaxErrorMessage());
	// }
	// System.out.println("parsed:\n" + node + "\n");
	// return node;
	// }
	//
	// private void shell() {
	// int linenum = 1;
	// String command = null;
	// while ((command = readLine()) != null) {
	// JCodeTree node = this.parse("<stdio>", linenum, command);
	// execute(node);
	// linenum += (command.split("\n").length);
	// }
	// }
	//
	// private static String readLine() {
	// ConsoleUtils.println("\n>>>");
	// Object console = ConsoleUtils.getConsoleReader();
	// StringBuilder sb = new StringBuilder();
	// while (true) {
	// String line = ConsoleUtils.readSingleLine(console, "   ");
	// if (line == null) {
	// return null;
	// }
	// if (line.equals("")) {
	// return sb.toString();
	// }
	// ConsoleUtils.addHistory(console, line);
	// sb.append(line);
	// sb.append("\n");
	// }
	// }

}
