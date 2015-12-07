package nez.ext;

import java.io.IOException;

import nez.main.Command;
import nez.main.CommandContext;
import nez.parser.hachi6.MozSpec;

public class Cgenvm extends Command {
	@Override
	public void exec(CommandContext config) throws IOException {
		MozSpec.genLoader();
	}
}
