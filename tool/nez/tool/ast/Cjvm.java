package nez.tool.ast;
//package nez.ext;
//
//import java.io.IOException;
//import java.lang.reflect.InvocationTargetException;
//import java.lang.reflect.Method;
//
//import nez.Grammar;
//import nez.Parser;
//import nez.ast.jcode.JCodeGenerator;
//import nez.ast.jcode.JCodeTree;
//import nez.ast.jcode.JCodeTreeTransducer;
//import nez.io.SourceContext;
//import nez.main.Command;
//import nez.main.CommandContext;
//import nez.util.ConsoleUtils;
//
//public class Cjvm extends Command {
//	public String getDesc() {
//		return "jvm";
//	}
//
//	private Grammar grammar;
//	private JCodeTreeTransducer treeTransducer;
//
//	@Override
//	public void exec(CommandContext config) throws IOException {
//		this.treeTransducer = new JCodeTreeTransducer();
//		this.grammar = config.newGrammar();
//		if (config.hasInput()) {
//			JCodeTree node = parse(config);
//			execute(node);
//		} else {
//			shell();
//		}
//	}
//
//	public final JCodeTree parse(CommandContext config) throws IOException {
//		Parser parser = config.newParser();
//		SourceContext source = config.nextInput();
//		JCodeTree node = (JCodeTree) parser.parse(source, this.treeTransducer);
//		if (node == null) {
//			ConsoleUtils.println(source.getSyntaxErrorMessage());
//		}
//		System.out.println("parsed:\n" + node + "\n");
//		return node;
//	}
//
//	public JCodeTree parse(String urn, int linenum, String text) {
//		Parser parser = grammar.newParser(text);
//		SourceContext source = SourceContext.newStringContext(urn, linenum, text);
//		JCodeTree node = (JCodeTree) parser.parse(source, treeTransducer);
//		if (node == null) {
//			ConsoleUtils.println(source.getSyntaxErrorMessage());
//		}
//		System.out.println("parsed:\n" + node + "\n");
//		return node;
//	}
//
//	private void shell() {
//		int linenum = 1;
//		String command = null;
//		while ((command = readLine()) != null) {
//			JCodeTree node = this.parse("<stdio>", linenum, command);
//			execute(node);
//			linenum += (command.split("\n").length);
//		}
//	}
//
//	private static String readLine() {
//		ConsoleUtils.println("\n>>>");
//		Object console = ConsoleUtils.getConsoleReader();
//		StringBuilder sb = new StringBuilder();
//		while (true) {
//			String line = ConsoleUtils.readSingleLine(console, "   ");
//			if (line == null) {
//				return null;
//			}
//			if (line.equals("")) {
//				return sb.toString();
//			}
//			ConsoleUtils.addHistory(console, line);
//			sb.append(line);
//			sb.append("\n");
//		}
//	}
//
//	public static void execute(JCodeTree node) {
//		JCodeGenerator generator = new JCodeGenerator("GeneratedClass");
//		generator.visit(node);
//		Class<?> mainClass = generator.generateClass();
//		try {
//			System.out.println("\n@@@@ Execute Byte Code @@@@");
//			Method method = mainClass.getMethod("main");
//			method.invoke(null);
//		} catch (NoSuchMethodException | SecurityException e) {
//			e.printStackTrace();
//		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
//			System.out.println("Invocation problem");
//			e.printStackTrace();
//		}
//	}
// }
