package nez.ext;

import java.io.IOException;

import nez.Parser;
import nez.main.Command;
import nez.main.CommandContext;
import nez.parser.NezCode;

public class Ccompile extends Command {
	@Override
	public void exec(CommandContext config) throws IOException {
		Parser parser = config.newParser();
		String path = config.getGrammarName() + ".moz";
		NezCode.writeMozCode(parser, path);
	}

}
