package nez.main;

import java.io.IOException;

import nez.ParserGenerator;
import nez.ast.Source;
import nez.ast.Tree;
import nez.lang.Grammar;
import nez.parser.Parser;
import nez.parser.io.CommonSource;
import nez.tool.ast.TreeWriter;
import nez.util.ConsoleUtils;
import nez.util.Verbose;

public class Cinez extends Command {
	Object console;
	String text = null;
	int linenum = 1;

	private void p(String line) {
		ConsoleUtils.begin(ConsoleUtils.Blue_);
		ConsoleUtils.println(line);
		ConsoleUtils.end();
	}

	@Override
	public void exec() throws IOException {
		Command.displayVersion();
		p("Enter an input string to match (or a grammar if you want to update).");
		p("Tips: Start with an empty line for multiple lines.");
		p("      Entering two empty lines diplays the current grammar.");
		ConsoleUtils.println("");
		Parser nezParser = this.getNezParser();
		TreeWriter writer = this.getTreeWriter("ast json xml", "ast");
		Grammar grammar = newGrammar();
		String start = grammar.getStartProduction().getLocalName();
		Parser p = strategy.newParser(grammar);
		p.setDisabledUnconsumed(false);
		ParserGenerator pg = new ParserGenerator();
		int startline = linenum;
		console = ReadLine.getConsoleReader();
		while (readText(ConsoleUtils.bold(start) + ">>> ") && text != null) {
			if (checkEmptyText(text)) {
				ConsoleUtils.begin(ConsoleUtils.Blue);
				grammar.dump();
				ConsoleUtils.end();
				continue;
			}
			Source sc = CommonSource.newStringSource("<stdio>", startline, text);
			startline = linenum;
			Tree<?> node = nezParser.parse(sc);
			if (node != null) {
				grammar = pg.newGrammar(node, "nez");
				try {
					start = grammar.getStartProduction().getLocalName();
					p = strategy.newParser(grammar);
					p.setDisabledUnconsumed(false);
					ReadLine.addHistory(console, text);
					p("Grammar is updated!");
					continue;
				} catch (Exception e) {
					Verbose.traceException(e);
				}
			}
			// System.out.println("parse " + p.hasErrors());
			node = p.parse(sc);
			if (p.hasErrors()) {
				ConsoleUtils.begin(ConsoleUtils.Red);
				p.showErrors();
				ConsoleUtils.end();
				if (node == null) {
					p("Tips: To enter multiple lines, start and end an empty line.");
				}
			}
			if (node != null) {
				ConsoleUtils.begin(ConsoleUtils.Blue);
				if (writer != null) {
					writer.writeTree(node);
				} else {
					ConsoleUtils.printlnIndent("   ", node.toString());
				}
				ConsoleUtils.end();
			}
		}
	}

	private boolean readText(String prompt) {
		StringBuilder sb = new StringBuilder();
		String line = ReadLine.readSingleLine(console, prompt);
		if (line == null) {
			text = null;
			return false;
		}
		int linecount = 0;
		boolean hasNext = false;
		if (line.equals("")) {
			hasNext = true;
		} else if (line.endsWith("\\")) {
			hasNext = true;
			sb.append(line.substring(0, line.length() - 1));
			linecount = 1;
		} else {
			linecount = 1;
			sb.append(line);
		}
		while (hasNext) {
			line = ReadLine.readSingleLine(console, "");
			if (line == null) {
				text = ""; // cancel
				return false;
			}
			if (line.equals("")) {
				break;
			} else if (line.endsWith("\\")) {
				if (linecount > 0) {
					sb.append("\n");
				}
				sb.append(line.substring(0, line.length() - 1));
				linecount++;
			} else {
				if (linecount > 0) {
					sb.append("\n");
				}
				sb.append(line);
				linecount++;
			}
		}
		if (linecount > 1) {
			sb.append("\n");
		}
		text = sb.toString();
		linenum += linecount;
		return true;
	}

	private boolean checkEmptyText(String s) {
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
				continue;
			}
			return false;
		}
		return true;
	}

}
