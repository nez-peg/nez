package nez.ext;

import java.io.IOException;
import java.util.HashMap;

import konoha.message.Message;
import nez.ast.script.EmptyResult;
import nez.ast.script.ScriptContext;
import nez.ast.script.ScriptRuntimeException;
import nez.main.Command;
import nez.main.CommandContext;
import nez.util.ConsoleUtils;

public class Ckonoha extends Command {
	public static boolean release = false;

	public final static void main(String[] args) {
		try {
			CommandContext c = new CommandContext();
			c.parseCommandOption(args, false/* nezCommand */);
			release = true;
			Command com = new Ckonoha();
			com.exec(c);
		} catch (IOException e) {
			ConsoleUtils.println(e);
			System.exit(1);
		}
	}

	@Override
	public void exec(CommandContext config) throws IOException {
		if (config.isUnspecifiedGrammarFilePath()) {
			config.setGrammarFilePath("konoha.nez");
		}
		ScriptContext sc = new ScriptContext(config.newParser());
		sc.setVerboseMode(!release);
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
		config.getStrategy();
		int linenum = 1;
		String command = null;
		while ((command = readLine()) != null) {
			if (command.trim().equals("")) {
				continue;
			}
			if (hasUTF8(command)) {
				ConsoleUtils.begin(36);
				ConsoleUtils.println(Message.DetectedUTF8);
				command = filterUTF8(command);
				ConsoleUtils.end();
			}
			try {
				ConsoleUtils.begin(32);
				Object result = sc.eval2("<stdio>", linenum, command);
				ConsoleUtils.end();
				if (!(result instanceof EmptyResult)) {
					ConsoleUtils.println("<<<");
					ConsoleUtils.bold();
					ConsoleUtils.println(result);
					ConsoleUtils.end();
				}
			} catch (ScriptRuntimeException e) {
				ConsoleUtils.begin(31);
				ConsoleUtils.println(e);
				e.printStackTrace();
				ConsoleUtils.end();
			} catch (RuntimeException e) {
				ConsoleUtils.begin(31);
				ConsoleUtils.println(e);
				e.printStackTrace();
				ConsoleUtils.end();
			}
			linenum += (command.split("\n").length);
		}
	}

	public final static String KonohaVersion = "4.0";

	private static void show(String name) {
		ConsoleUtils.bold();
		ConsoleUtils.println("Konoha U (" + name + ") on Nez " + Version);
		ConsoleUtils.end();
		ConsoleUtils.println(Copyright);
		ConsoleUtils.println("Copyright (c) 2015, Kimio Kuramitsu, Yokohama National University");
		ConsoleUtils.begin(37);
		ConsoleUtils.println(Message.Hint);
		ConsoleUtils.end();
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

	private boolean hasUTF8(String command) {
		boolean skip = false;
		for (int i = 0; i < command.length(); i++) {
			char c = command.charAt(i);
			if (c == '"') {
				skip = !skip;
				continue;
			}
			if (c < 128 || skip) {
				continue;
			}
			return true;
		}
		return false;
	}

	HashMap<Character, Character> charMap = null;

	void initCharMap() {
		if (charMap == null) {
			charMap = new HashMap<>();
			charMap.put('　', ' ');
			charMap.put('（', '(');
			charMap.put('）', ')');
			charMap.put('［', '[');
			charMap.put('］', ']');
			charMap.put('｛', '{');
			charMap.put('｝', '}');
			charMap.put('”', '"');
			charMap.put('’', '\'');
			charMap.put('＜', '<');
			charMap.put('＞', '>');
			charMap.put('＋', '+');
			charMap.put('ー', '-');
			charMap.put('＊', '*');
			charMap.put('／', '/');
			charMap.put('✕', '*');
			charMap.put('÷', '/');
			charMap.put('＝', '=');
			charMap.put('％', '%');
			charMap.put('？', '?');
			charMap.put(':', ':');
			charMap.put('＆', '&');
			charMap.put('｜', '|');
			charMap.put('！', '!');
			charMap.put('、', ',');
			charMap.put('；', ';');
			charMap.put('。', '.');
			for (char c = 'A'; c <= 'Z'; c++) {
				charMap.put((char) ('Ａ' + (c - 'A')), c);
			}
			for (char c = 'a'; c <= 'z'; c++) {
				charMap.put((char) ('ａ' + (c - 'a')), c);
			}
			for (char c = '0'; c <= '9'; c++) {
				charMap.put((char) ('０' + (c - '0')), c);
			}
			for (char c = '1'; c <= '9'; c++) {
				charMap.put((char) ('一' + (c - '0')), c);
			}
		}
	}

	private String filterUTF8(String command) {
		initCharMap();
		StringBuilder sb = new StringBuilder(command.length());
		boolean skip = false;
		for (int i = 0; i < command.length(); i++) {
			char c = command.charAt(i);
			if (c < 128 || skip) {
				if (c == '"') {
					skip = !skip;
				}
				sb.append(c);
				continue;
			}
			Character mapped = charMap.get(c);
			if (mapped != null) {
				sb.append(mapped);
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

}
