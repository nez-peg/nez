package nez.ext;

import java.io.IOException;
import java.util.TreeMap;

import nez.main.Command;
import nez.parser.Parser;
import nez.parser.ParserGenerator;
import nez.util.ConsoleUtils;

public class Cparser extends Command {
	private static final TreeMap<String, Class<?>> classMap;
	static {
		classMap = new TreeMap<String, Class<?>>();
		classMap.put("coffee", nez.x.generator.CoffeeParserGenerator.class);
		classMap.put("peg", nez.parser.generator.PEGGenerator.class);
		classMap.put("c", nez.parser.generator.CParserGenerator.class);
	}

	@Override
	public void exec(CommandContext config) throws IOException {
		ParserGenerator pgen = newParserGenerator(config);
		Parser p = config.newParser();
		pgen.generate(p.getParserGrammar());
	}

	protected ParserGenerator newParserGenerator(CommandContext config) {
		String ext = config.getCommandExtension();
		if (ext == null) {
			ext = "unspecified";
		}
		ParserGenerator pgen = config.newParserGenerator(classMap.get(ext));
		if (pgen == null) {
			ConsoleUtils.println("parser.xxx:");
			for (String k : classMap.keySet()) {
				ConsoleUtils.println(String.format(" %-10s %s", k, classMap.get(k).getName()));
			}
			ConsoleUtils.exit(1, "undefined parser extension: " + ext);
		}
		return pgen;
	}

}
