package nez.main;

import java.io.IOException;

import nez.parser.Parser;
import nez.parser.ParserCode;

public class Ccompile extends Command {
	@Override
	public void exec() throws IOException {
		Parser parser = newParser();
		ParserCode<?> code = parser.compile();
		code.dump();
	}
}
