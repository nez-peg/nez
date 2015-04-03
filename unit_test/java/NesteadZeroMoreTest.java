import nez.main.Command;
import nez.main.CommandConfigure;

import org.junit.Test;


public class NesteadZeroMoreTest {
	@Test(timeout=5000)
	public void test() {
		final String pegRule = 
				"File = B*\n" +
				"B = '1'*";
		final String[] args = new String[] {
				"parse", "-t", "111", "-e", pegRule	
		};
		CommandConfigure config = new CommandConfigure();
		config.parseCommandOption(args);
		Command com = config.getCommand();
		com.exec(config);
		// FIXME assert
	}
}
