package nez.ext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import nez.Grammar;
import nez.Parser;
import nez.Strategy;
import nez.ast.CommonTree;
import nez.io.SourceContext;
import nez.lang.Formatter;
import nez.lang.GrammarFile;
import nez.lang.Production;
import nez.main.Command;
import nez.main.CommandContext;
import nez.util.ConsoleUtils;
import nez.x.generator.GeneratorLoader;

public class Cshell extends Command {
	String command = null;
	String text = null;
	int linenum = 0;

	@Override
	public void exec(CommandContext config) throws IOException {
		Command.displayVersion();
		Grammar g = config.newGrammar();
		ConsoleUtils.addCompleter(getNonterminalList(g));
		Strategy option = config.getStrategy();

		while (readLine(">>> ")) {
			if ((command != null && command.equals(""))) {
				continue;
			}
			// System.out.println("command: " + command);
			// System.out.println("text: " + text);
			if (command == null) {
				defineProduction(g, text);
				continue;
			}
			if (text != null && GeneratorLoader.isSupported(command)) {
				Parser p = g.newParser(text);
				if (p != null) {
					execCommand(command, p, option);
				}
				continue;
			}
			Parser p = g.newParser(command);
			if (p == null) {
				continue;
			}
			if (text == null) {
				displayGrammar(command, p);
			} else {
				SourceContext sc = SourceContext.newStringSourceContext("<stdio>", linenum, text);
				CommonTree node = p.parseCommonTree(sc);
				if (node == null) {
					ConsoleUtils.println(sc.getSyntaxErrorMessage());
					continue;
				}
				if (sc.hasUnconsumed()) {
					ConsoleUtils.println(sc.getUnconsumedMessage());
				}
				sc = null;
				ConsoleUtils.println(node.toString());
				if (g instanceof GrammarFile) {
					GrammarFile gfile2 = (GrammarFile) g;
					if (Formatter.isSupported(gfile2, node)) {
						ConsoleUtils.println("Formatted: " + Formatter.format(gfile2, node));
					}
				}
			}
		}
	}

	public final List<String> getNonterminalList(Grammar g) {
		ArrayList<String> l = new ArrayList<String>();
		for (Production p : g) {
			String s = p.getLocalName();
			char c = s.charAt(0);
			if (!Character.isUpperCase(c)) {
				continue;
			}
			l.add(s);
		}
		Collections.sort(l);
		return l;
	}

	private void displayGrammar(String command, Parser g) {
		g.getGrammar().dump();
	}

	private int indexOfOperator(String line) {
		int index = -1;
		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			if (c == '|' || c == '<' || c == '=') {
				return i;
			}
			if (c == ' ' && index == -1) {
				index = i;
			}
		}
		return index;
	}

	private boolean readLine(String prompt) {
		Object console = ConsoleUtils.getConsoleReader();
		String line = ConsoleUtils.readSingleLine(console, prompt);
		if (line == null) {
			return false;
		}
		int loc = indexOfOperator(line);
		if (loc != -1) {
			command = line.substring(0, loc).trim();
			text = line.substring(loc).trim();
			ConsoleUtils.addHistory(console, line);
		} else {
			command = line.trim();
			text = null;
			ConsoleUtils.addHistory(console, line);
			return true;
		}
		linenum++;
		if (text.startsWith("<")) {
			ConsoleUtils.addHistory(console, line);
			String delim = text.substring(1);
			StringBuilder sb = new StringBuilder();
			while ((line = ConsoleUtils.readSingleLine(console, "")) != null) {
				if (line.startsWith(delim)) {
					break;
				}
				sb.append(line);
				sb.append("\n");
			}
			text = sb.toString();
			return true;
		}
		if (text.startsWith("=") || command.equals("import") || command.equals("format") || command.equals("example")) {
			StringBuilder sb = new StringBuilder();
			sb.append(line);
			sb.append("\n");
			while ((line = ConsoleUtils.readSingleLine(console, "... ")) != null) {
				if (line.equals("")) {
					break;
				}
				sb.append(line);
				sb.append("\n");
			}
			command = null;
			text = sb.toString();
			return true;
		}
		ConsoleUtils.addHistory(console, line);
		return true;
	}

	private Parser newParser(GrammarFile file, String text) {
		String name = text.replace('\n', ' ').trim();
		Parser p = file.newParser(name);
		if (p == null) {
			ConsoleUtils.println("NameError: name '" + name + "' is not defined");
		}
		return p;
	}

	private void defineProduction(Grammar g, String text) {
		// ConsoleUtils.println("--\n"+text+"--");
		Gnez loader = new Gnez();
		loader.eval(g, "<stdio>", linenum, text, null);
		ConsoleUtils.addCompleter(getNonterminalList(g));
	}

	static HashMap<String, ShellCommand> cmdMap = new HashMap<String, ShellCommand>();
	static {
		cmdMap.put("nez", new NezCommand());
	}

	static boolean hasCommand(String cmd) {
		return cmdMap.containsKey(cmd);
	}

	static void execCommand(String cmd, Parser g, Strategy option) {
		// ParserGenerator gen = GeneratorLoader.load(cmd);
		// gen.generate(g, option, null);
		// ConsoleUtils.println("");
	}
}

abstract class ShellCommand {
	public abstract void perform(Parser g);
}

class NezCommand extends ShellCommand {

	@Override
	public void perform(Parser g) {
		// NezGrammarGenerator gen = new NezGrammarGenerator();
		// for (Production p : g.getProductionList()) {
		// gen.visitProduction(p);
		// }
	}

}
