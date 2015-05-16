package nez.main;

import java.io.IOException;
import java.util.HashMap;

import nez.SourceContext;
import nez.ast.CommonTree;
import nez.ast.CommonTreeWriter;
import nez.generator.NezGenerator;
import nez.generator.NezGrammarGenerator;
import nez.lang.Formatter;
import nez.lang.Grammar;
import nez.lang.NameSpace;
import nez.lang.NezParser;
import nez.lang.Production;
import nez.util.ConsoleUtils;
import nez.util.UList;

public class NezInteractiveParser extends Command {
	@Override
	public final String getDesc() {
		return "starts an interactive parser";
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
			if((command != null && command.equals(""))) {
				continue;
			}
//			System.out.println("command: " + command);
//			System.out.println("text: " + text);
			if(command == null) {
				defineProduction(ns, text);
				continue;
			}
			if(text != null && NezGenerator.supportedGenerator(command)) {
				Grammar g = getGrammar(ns, text);
				if(g != null) {
					execCommand(command, g);
				}
				continue;
			}
			Grammar g = getGrammar(ns, command);
			if(g == null) {
				continue;
			}
			if(text == null) {
				displayGrammar(command, g);
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
				if(Formatter.isSupported(ns, node)) {
					ConsoleUtils.println("Formatted: " + Formatter.format(ns, node));
				}
			}
		}
	}
	
	private void displayGrammar(String command, Grammar g) {
		g.getStartProduction().dump();
	}
	
	private int indexOfOperator(String line) {
		int index = -1;
		for(int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			if(c == '|' || c == '<' || c == '=') {
				return i;
			}
			if(c == ' ' && index == -1) {
				index = i;
			}
		}
		return index;
	}

	private boolean readLine(String prompt) {
		Object console = ConsoleUtils.getConsoleReader();
		String line = ConsoleUtils.readSingleLine(console, prompt);
		if(line == null) {
			return false;
		}
		int loc = indexOfOperator(line);
		if(loc != -1) {
			command = line.substring(0, loc).trim();
			text = line.substring(loc).trim();
			ConsoleUtils.addHistory(console, line);
		}
		else {
			command = line.trim();
			text = null;
			ConsoleUtils.addHistory(console, line);
			return true;
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
		//ConsoleUtils.println("--\n"+text+"--");
		NezParser parser = new NezParser();
		parser.eval(ns, "<stdio>", linenum, text);
		ConsoleUtils.addCompleter(ns.getNonterminalList());
	}
	
	static HashMap<String, ShellCommand> cmdMap = new HashMap<String,ShellCommand>();
	static {
		cmdMap.put("nez", new NezCommand());
	}
	
	static boolean hasCommand(String cmd) {
		return cmdMap.containsKey(cmd);
	}
	
	static void execCommand(String cmd, Grammar g) {
		NezGenerator gen = NezGenerator.newNezGenerator(cmd);
		gen.generate(g);
		ConsoleUtils.println("");
	}
}

abstract class ShellCommand {
	public abstract void perform(Grammar g);
}

class NezCommand extends ShellCommand {

	@Override
	public void perform(Grammar g) {
		NezGrammarGenerator gen  = new NezGrammarGenerator(null);
		for(Production p: g.getSubProductionList()) {
			gen.visitProduction(p);
		}
	}
	
}

