package nez.ext;

import java.io.IOException;

import nez.ast.script.EmptyResult;
import nez.ast.script.ScriptContext;
import nez.ast.script.ScriptRuntimeException;
import nez.main.Command;
import nez.main.CommandContext;
import nez.util.ConsoleUtils;

public class Ckonoha extends Command {

	@Override
	public void exec(CommandContext config) throws IOException {
		if (config.isUnspecifiedGrammarFilePath()) {
			config.setGrammarFilePath("konoha.nez");
		}
		ScriptContext sc = new ScriptContext(config.newParser());
		if (config.hasInput()) {
			while (config.hasInput()) {
				sc.eval(config.nextInput());
			}
		} else {
			shell(config, sc);
		}
	}

	public void shell(CommandContext config, ScriptContext sc) {
		show(config.newGrammar().getDesc());
		sc.setShellMode(true);
		sc.setVerboseMode(true);
		config.getStrategy();
		int linenum = 1;
		String command = null;
		while ((command = readLine()) != null) {
			if (command.trim().equals("")) {
				continue;
			}
			try {
				Object result = sc.eval2("<stdio>", linenum, command);
				if (!(result instanceof EmptyResult)) {
					ConsoleUtils.println("<<<");
					ConsoleUtils.println(result);
				}
			} catch (ScriptRuntimeException e) {
				ConsoleUtils.println(e.getMessage());
			} catch (RuntimeException e) {
				ConsoleUtils.println(e);
				e.printStackTrace();
			}
			linenum += (command.split("\n").length);
		}
	}

	public final static String KonohaVersion = "4.0";

	private static void show(String name) {
		ConsoleUtils.println("Konoha 4E (" + name + ") on Nez " + Version);
		ConsoleUtils.println(Copyright);
		ConsoleUtils.println("Copyright (c) 2015, Kimio Kuramitsu, Yokohama National University");
	}

	private static String readLine() {
		ConsoleUtils.println(">>>");
		Object console = ConsoleUtils.getConsoleReader();
		StringBuilder sb = new StringBuilder();
		while (true) {
			String line = ConsoleUtils.readSingleLine(console, "");
			if (line == null) {
				return null;
			}
			if (line.equals("")) {
				return sb.toString();
			}
			ConsoleUtils.addHistory(console, line);
			sb.append(line);
			sb.append("\n");
		}
	}

}
