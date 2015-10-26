package nez.main;

import java.io.IOException;

import nez.Version;
import nez.util.ConsoleUtils;

public abstract class Command {
	public final static String ProgName = "Nez";
	public final static String CodeName = "yokohama";
	public final static int MajorVersion = 0;
	public final static int MinerVersion = 9;
	public final static int PatchLevel = nez.Version.REV;
	public static void main(String[] args) {
		try {
			CommandContext c = new CommandContext();
			c.parseCommandOption(args, true/* nezCommand */);
			Command com = c.newCommand();
			com.exec(c);
		} catch (IOException e) {
			ConsoleUtils.println(e);
			System.exit(1);
		}
	}

	public abstract void exec(CommandContext config) throws IOException;

	public final static void displayVersion() {
		ConsoleUtils.println(ProgName + "-" + nez.Version.Version + " (" + CodeName + ") on Java JVM-" + System.getProperty("java.version"));
		ConsoleUtils.println(Version.Copyright);
	}

}
