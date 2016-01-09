package nez.tool.ast;

import java.io.IOException;

import nez.main.Command;
import nez.util.ConsoleUtils;

public class Ckonoha extends Command {
	public static boolean release = false;

	@Override
	public void exec() throws IOException {
		ConsoleUtils.println("use konoha.jar");
	}

}
