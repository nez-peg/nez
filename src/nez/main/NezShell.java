package nez.main;

import java.io.IOException;

import nez.SourceContext;
import nez.ast.CommonTree;
import nez.ast.CommonTreeWriter;
import nez.lang.Grammar;
import nez.lang.NameSpace;
import nez.lang.NezParser;
import nez.lang.Production;
import nez.util.ConsoleUtils;
import nez.util.UList;

public class NezShell extends Command {
	@Override
	public final String getDesc() {
		return "an interactive parser";
	}

	String command = null;
	String text = null;
	int linenum = 0;
	
	@Override
	public void exec(CommandConfigure config) {
		Command.displayVersion();
		NameSpace ns = config.getNameSpace(true);
		ConsoleUtils.addCompleter(ns.getNonterminalList());
		while(readLine(">>> ")) {
			if(command == null) {
				defineProduction(ns, text);
			}
			else {
				Grammar g = getGrammar(ns, command);
				if(g == null) {
					continue;
				}
				if(text == null) {
					ConsoleUtils.println(g);
				}
				else {
					SourceContext sc = SourceContext.newStringSourceContext("<stdio>", linenum, text);
					CommonTree node = g.parse(sc);
					if(node == null) {
						ConsoleUtils.println(sc.getSyntaxErrorMessage());
						continue;
					}
					if(sc.hasUnconsumed()) {
						ConsoleUtils.println(sc.getUnconsumedMessage());
					}
					sc = null;
					new CommonTreeWriter().transform(null, node);
				}
			}
		}
	}
	
	private boolean readLine(String prompt) {
		Object console = ConsoleUtils.getConsoleReader();
		String line = ConsoleUtils.readSingleLine(console, prompt);
		if(line == null) {
			return false;
		}
		int loc = line.indexOf(' ');
		if(loc != -1) {
			command = line.substring(0, loc).trim();
			text = line.substring(loc+1).trim();
			ConsoleUtils.addHistory(console, line);
		}
		else {
			loc = line.indexOf('=');
			if(loc == -1) {
				loc = line.indexOf('<');
			}
			if(loc == -1) {
				command = line.trim();
				text = null;
				ConsoleUtils.addHistory(console, line);
				return true;
			}
			command = line.substring(0, loc).trim();
			text = line.substring(loc).trim();
		}
		linenum++;
		if(text.startsWith("<")) {
			ConsoleUtils.addHistory(console, line);
			String delim = text.substring(1);
			StringBuilder sb = new StringBuilder();
			while((line = ConsoleUtils.readSingleLine(console, "")) != null) {
				if(line.startsWith(delim)) {
					break;
				}
				sb.append(line);
				sb.append("\n");
			}
			text = sb.toString();
			return true;
		}
		if(text.startsWith("=") || command.equals("import") || command.equals("format") || command.equals("example")) {
			StringBuilder sb = new StringBuilder();
			sb.append(line); 
			sb.append("\n");
			while((line = ConsoleUtils.readSingleLine(console, "... ")) != null) {
				if(line.equals("")) {
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

	private Grammar getGrammar(NameSpace ns, String text) {
		String name = text.replace('\n', ' ').trim();
		Grammar g = ns.newGrammar(name);
		if(g == null) {
			ConsoleUtils.println("NameError: name '"+name+"' is not defined");
		}
		return g;
	}
	
	private void defineProduction(NameSpace ns, String text) {
		ConsoleUtils.println("--\n"+text+"--");
		NezParser parser = new NezParser();
		parser.eval(ns, "<stdio>", linenum, text);
		ConsoleUtils.addCompleter(ns.getNonterminalList());
	}
	
}
