package nez.ext;

import java.io.IOException;
import java.lang.reflect.Method;

import nez.main.Command;
import nez.main.CommandContext;
import nez.util.ConsoleUtils;

public class Ckonoha extends Command {
	@Override
	public void exec(CommandContext config) throws IOException {
		Class<?> c = null;
		try {
			c = Class.forName("nez.konoha.Konoha");
		} catch (ClassNotFoundException e) {
			ConsoleUtils.exit(1, "unsupported konoha");
		}
		try {
			Object konoha = c.newInstance();
			Method shell = c.getMethod("shell");
			shell.invoke(konoha);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
