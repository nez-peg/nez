package nez.tool.ast;

import java.io.IOException;

import nez.junks.MozSpec;
import nez.main.Command;

public class Cgenvm extends Command {
	@Override
	public void exec() throws IOException {
		MozSpec.genLoader();
	}
}
