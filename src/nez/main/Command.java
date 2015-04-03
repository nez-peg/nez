package nez.main;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.TreeMap;

import nez.util.ConsoleUtils;


public abstract class Command {
	public final static String  ProgName  = "Nez";
	public final static String  CodeName  = "yokohama";
	public final static int     MajorVersion = 1;
	public final static int     MinerVersion = 0;
	public final static int     PatchLevel   = 0;
	public final static String  Version = "" + MajorVersion + "." + MinerVersion + "." + PatchLevel;
	public final static String  Copyright = "Copyright (c) 2014-2015, Nez project authors";
	public final static String  License = "BSD-Style Open Source";

	public final static void main(String[] args) {
		CommandConfigure config = new CommandConfigure();
		config.parseCommandOption(args);
		Command com = config.getCommand();
		com.exec(config);
	}

	public abstract void exec(CommandConfigure config);
	
	public final static void displayVersion() {
		ConsoleUtils.println(ProgName + "-" + Version + " (" + CodeName + ") on Java JVM-" + System.getProperty("java.version"));
		ConsoleUtils.println(Copyright);
	}
	
	private static jline.ConsoleReader ConsoleReader = null;

	public final static String readMultiLine(String prompt, String prompt2) {
		if(ConsoleReader == null) {
			displayVersion();
			try {
				ConsoleReader = new jline.ConsoleReader();
			}
			catch(IOException e) {
				throw new RuntimeException(e);
			}
		}
		StringBuilder sb = new StringBuilder();
		String line;
		while(true) {
			line = readSingleLine(prompt);
			if(line == null) {
				return null;
			}
			if(!line.endsWith("\\")) {
				sb.append(line);
				break;
			}
			sb.append(line.substring(0, line.length() - 1));
			sb.append("\n");
			prompt = prompt2;
		}
		line = sb.toString();
		ConsoleReader.getHistory().addToHistory(line);
		return line;
	}

	private final static String readSingleLine(String prompt) {
		try {
			return ConsoleReader.readLine(prompt);
		}
		catch(IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	// command database 
	
	private static TreeMap<String,Command> commandTable = new TreeMap<String,Command>();

	public static void load(String name, String className) {
		try {
			Class<?> c = Class.forName(className);
			commandTable.put(name, (Command)c.newInstance());
		}
		catch(Exception e) {
			Verbose.println("undefined command: " + name + " due to " + e);
		}
	}

	static {
		load("check", "nez.main.CheckCommand");
		load("parse", "nez.main.ParseCommand");
		load("rel", "nez.x.RelationCommand");
		load("cc", "nez.cc.GeneratorCommand");
		load("peg", "nez.cc.GrammarCommand");
		load("conv", "nez.x.ConverterCommand");
		load("regex", "nez.x.RegexCommand");
		load("demo", "nez.x.DemoCommand");
		load("type", "nez.x.TypeCommand");
		load("dtd", "nez.x.DTDCommand");
	}
	
	public static final Command getCommand(String name) {
		return commandTable.get(name);
	}

	public static void showList() {
		for(Entry<String,Command> e : commandTable.entrySet()) {
			Command c = e.getValue();
			ConsoleUtils.println(String.format("  %8s - %s", e.getKey(), c.getDesc()));
		}
	}

	public abstract String getDesc();

}

