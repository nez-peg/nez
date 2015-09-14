import java.io.IOException;

import nez.main.Command;
import nez.main.CommandContext;

import org.junit.Test;

public class NesteadZeroMoreTest {
	@Test(timeout = 5000)
	public void test() throws IOException {
		final String pegRule = "File = B*\n" + "B = '1'*";
		final String[] args = new String[] { "parse", "-t", "111", "-e", pegRule };
		CommandContext config = new CommandContext();
		config.parseCommandOption(args);
		Command com = config.newCommand();
		com.exec(config);
		// FIXME assert
	}
}
