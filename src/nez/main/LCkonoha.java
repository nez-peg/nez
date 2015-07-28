package nez.main;


import java.lang.reflect.Method;

import nez.util.ConsoleUtils;

public class LCkonoha extends Command {
	@Override
	public String getDesc() {
		return "a konoha interpreter";
	}

	@Override
	public void exec(CommandContext config) {
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
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

}
