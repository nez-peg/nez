package nez.ext;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import nez.main.Command;
import nez.main.CommandContext;
import nez.parser.hachi6.Moz;

public class Cmozdump extends Command {
	@Override
	public void exec(CommandContext config) throws IOException {
		File f = new File(config.getGrammarPath());
		byte[] buf = new byte[(int) f.length()];
		FileInputStream fis = new FileInputStream(f);
		fis.read(buf);
		fis.close();

		System.out.println("Moz dump");
		Moz.dump(buf);
	}

}
