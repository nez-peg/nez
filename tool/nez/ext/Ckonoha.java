package nez.ext;

import java.io.IOException;

import nez.main.Command;
import nez.main.CommandContext;
import nez.util.ConsoleUtils;

public class Ckonoha extends Command {
	public static boolean release = false;

	@Override
	public void exec(CommandContext config) throws IOException {
		ConsoleUtils.println("use konoha.jar");
	}

}
