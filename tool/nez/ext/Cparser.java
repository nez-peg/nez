//package nez.ext;
//
//import java.io.IOException;
//import java.util.TreeMap;
//
//import nez.main.Command;
//import nez.parser.Parser;
//import nez.parser.GrammarWriter;
//import nez.util.ConsoleUtils;
//
//public class Cparser extends Command {
//	private static final TreeMap<String, Class<?>> classMap;
//	static {
//		classMap = new TreeMap<String, Class<?>>();
//		classMap.put("coffee", nez.x.generator.CoffeeParserGenerator.class);
//		classMap.put("peg", nez.grammar.ParsingExpressionGrammarWriter.class);
//		classMap.put("c", nez.grammar.CParserGenerator.class);
//	}
//
//	@Override
//	public void exec(CommandContext config) throws IOException {
//		GrammarWriter pgen = newParserGenerator(config);
//		Parser p = config.newParser();
//		pgen.generate(p.getParserGrammar());
//	}
//
//	protected GrammarWriter newParserGenerator(CommandContext config) {
//		String ext = config.getCommandExtension();
//		if (ext == null) {
//			ext = "unspecified";
//		}
//		GrammarWriter pgen = config.newParserGenerator(classMap.get(ext));
//		if (pgen == null) {
//			ConsoleUtils.println("parser.xxx:");
//			for (String k : classMap.keySet()) {
//				ConsoleUtils.println(String.format(" %-10s %s", k, classMap.get(k).getName()));
//			}
//			ConsoleUtils.exit(1, "undefined parser extension: " + ext);
//		}
//		return pgen;
//	}
//
// }
