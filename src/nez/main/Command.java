package nez.main;

import nez.util.ConsoleUtils;


public abstract class Command {
	public final static boolean ReleasePreview = true;
	public final static String  ProgName  = "Nez";
	public final static String  CodeName  = "yokohama";
	public final static int     MajorVersion = 0;
	public final static int     MinerVersion = 9;
	public final static int     PatchLevel   = 0;
	public final static String  Version = "" + MajorVersion + "." + MinerVersion + "." + PatchLevel;
	public final static String  Copyright = "Copyright (c) 2014-2015, Nez project authors";
	public final static String  License = "BSD-License Open Source";

	public final static void main(String[] args) {
		CommandContext c = new CommandContext(args);
		Command com = c.getCommand();
		com.exec(c);
	}

	public abstract String getDesc();
	public abstract void exec(CommandContext config);
	
	public final static void displayVersion() {
		ConsoleUtils.println(ProgName + "-" + Version + " (" + CodeName + ") on Java JVM-" + System.getProperty("java.version"));
		ConsoleUtils.println(Copyright);
	}

}

