package nez.main;

import java.io.IOException;

import nez.ast.CommonTree;
import nez.ast.Source;
import nez.debugger.DebugManager;
import nez.ext.CommandContext;
import nez.io.CommonSource;
import nez.lang.Grammar;
import nez.parser.Parser;
import nez.util.ConsoleUtils;

public class Cshell extends Command {
	String text = null;
	int linenum = 0;

	@Override
	public void exec() throws IOException {
		Command.displayVersion();
		Grammar grammar = newGrammar();
		String start = grammar.getStartProduction().getLocalName();
		Parser p = strategy.newParser(grammar);
		p.setDisabledUnconsumed(true);
		while (readLine(start + ">>> ")) {
			Source sc = CommonSource.newStringSource("<stdio>", linenum, text);
			CommonTree node = p.parse(sc);
			if (node == null || p.hasErrors()) {
				p.showErrors();
				// FIXME activateNezDebugger(config);
				p.clearErrors();
				continue;
			}
			sc = null;
			ConsoleUtils.printlnIndent("   ", node.toString());
			// if (Formatter.isSupported(g, node)) {
			// ConsoleUtils.println("Formatted: " + Formatter.format(g, node));
			// }
		}
	}

	private boolean readLine(String prompt) {
		Object console = ReadLine.getConsoleReader();
		StringBuilder sb = new StringBuilder();
		String line = ReadLine.readSingleLine(console, prompt);
		if (line == null) {
			return false;
		}
		sb.append(line);
		ReadLine.addHistory(console, line);
		while (true) {
			line = ReadLine.readSingleLine(console, "...");
			if (line == null) {
				return false;
			}
			if (line.equals("")) {
				text = sb.toString();
				return true;
			}
			sb.append(line);
			ReadLine.addHistory(console, line);
			sb.append("\n");
		}
	}

	private boolean readActivateDebugger() {
		Object console = ReadLine.getConsoleReader();
		while (true) {
			String line = ReadLine.readSingleLine(console, "Do you want to start the Nez debugger? (yes/no)");
			if (line == null) {
				ConsoleUtils.println("Please push the key of yes or no. You input the key: " + line);
				continue;
			}
			if (line.equals("yes") || line.equals("y")) {
				return true;
			}
			if (line.equals("no") || line.equals("n")) {
				return false;
			}
			ConsoleUtils.println("Please push the key of yes or no. You input the key: " + line);
		}
	}

	private void activateNezDebugger(CommandContext config) {
		if (readActivateDebugger()) {
			Parser parser = config.newParser();
			DebugManager manager = new DebugManager(text);
			manager.exec(parser, config.getStrategy());
		}
	}
}
